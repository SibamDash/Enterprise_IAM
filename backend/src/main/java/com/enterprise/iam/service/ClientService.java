package com.enterprise.iam.service;

import com.enterprise.iam.dto.client.ClientDto;
import com.enterprise.iam.dto.client.ClientSecretResponse;
import com.enterprise.iam.dto.client.CreateClientRequest;
import com.enterprise.iam.dto.client.UpdateClientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final RegisteredClientRepository registeredClientRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<ClientDto> getAllClients() {
        List<String> ids = jdbcTemplate.queryForList("SELECT id FROM oauth2_registered_client", String.class);
        return ids.stream()
                .map(registeredClientRepository::findById)
                .filter(java.util.Objects::nonNull)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientDto getClientById(String id) {
        RegisteredClient client = registeredClientRepository.findById(id);
        if (client == null) {
            throw new IllegalArgumentException("Client not found");
        }
        return mapToDto(client);
    }

    @Transactional
    public ClientSecretResponse createClient(CreateClientRequest request) {
        if (registeredClientRepository.findByClientId(request.getClientId()) != null) {
            throw new IllegalArgumentException("Client ID already exists");
        }

        String rawSecret = generateSecret();
        String encodedSecret = "{bcrypt}" + passwordEncoder.encode(rawSecret);

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(request.getClientId())
                .clientSecret(encodedSecret)
                .clientIdIssuedAt(Instant.now())
                .clientName(request.getClientName());

        request.getClientAuthenticationMethods().forEach(m -> builder.clientAuthenticationMethod(new ClientAuthenticationMethod(m)));
        request.getAuthorizationGrantTypes().forEach(g -> builder.authorizationGrantType(new AuthorizationGrantType(g)));
        
        if (request.getRedirectUris() != null) {
            request.getRedirectUris().forEach(builder::redirectUri);
        }
        
        if (request.getPostLogoutRedirectUris() != null) {
            request.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        }
        
        if (request.getScopes() != null) {
            request.getScopes().forEach(builder::scope);
        }

        // Default settings for our enterprise scenario
        builder.clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(false)
                .build());

        builder.tokenSettings(TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(Duration.ofMinutes(5))
                .refreshTokenTimeToLive(Duration.ofMinutes(60))
                .reuseRefreshTokens(true)
                .build());

        RegisteredClient registeredClient = builder.build();
        registeredClientRepository.save(registeredClient);

        return ClientSecretResponse.builder()
                .id(registeredClient.getId())
                .clientId(registeredClient.getClientId())
                .clientSecret(rawSecret) // Return raw secret only once
                .build();
    }

    @Transactional
    public ClientDto updateClient(String id, UpdateClientRequest request) {
        RegisteredClient existing = registeredClientRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Client not found");
        }

        RegisteredClient.Builder builder = RegisteredClient.from(existing)
                .clientName(request.getClientName());

        // We must completely replace methods, grants, uris and scopes.
        // Spring Security RegisteredClient.from() copies them, and the builder adds to them.
        // To replace, we might need a workaround or we accept that it appends.
        // Actually, RegisteredClient.Builder does not have methods to clear existing sets easily except by rebuilding manually.
        // Let's create a fresh builder with the existing ID, clientId, and secret to cleanly set the collections.
        
        RegisteredClient.Builder cleanBuilder = RegisteredClient.withId(existing.getId())
                .clientId(existing.getClientId())
                .clientSecret(existing.getClientSecret())
                .clientIdIssuedAt(existing.getClientIdIssuedAt())
                .clientName(request.getClientName())
                .clientSettings(existing.getClientSettings())
                .tokenSettings(existing.getTokenSettings());

        request.getClientAuthenticationMethods().forEach(m -> cleanBuilder.clientAuthenticationMethod(new ClientAuthenticationMethod(m)));
        request.getAuthorizationGrantTypes().forEach(g -> cleanBuilder.authorizationGrantType(new AuthorizationGrantType(g)));
        
        if (request.getRedirectUris() != null) {
            request.getRedirectUris().forEach(cleanBuilder::redirectUri);
        }
        if (request.getPostLogoutRedirectUris() != null) {
            request.getPostLogoutRedirectUris().forEach(cleanBuilder::postLogoutRedirectUri);
        }
        if (request.getScopes() != null) {
            request.getScopes().forEach(cleanBuilder::scope);
        }

        RegisteredClient updatedClient = cleanBuilder.build();
        registeredClientRepository.save(updatedClient);

        return mapToDto(updatedClient);
    }

    @Transactional
    public ClientSecretResponse rotateClientSecret(String id) {
        RegisteredClient existing = registeredClientRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Client not found");
        }

        String rawSecret = generateSecret();
        String encodedSecret = "{bcrypt}" + passwordEncoder.encode(rawSecret);

        RegisteredClient updatedClient = RegisteredClient.from(existing)
                .clientSecret(encodedSecret)
                .build();

        registeredClientRepository.save(updatedClient);

        return ClientSecretResponse.builder()
                .id(updatedClient.getId())
                .clientId(updatedClient.getClientId())
                .clientSecret(rawSecret)
                .build();
    }

    @Transactional
    public void deleteClient(String id) {
        int rows = jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE id = ?", id);
        if (rows == 0) {
            throw new IllegalArgumentException("Client not found");
        }
    }

    private String generateSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private ClientDto mapToDto(RegisteredClient client) {
        return ClientDto.builder()
                .id(client.getId())
                .clientId(client.getClientId())
                .clientName(client.getClientName())
                .clientIdIssuedAt(client.getClientIdIssuedAt())
                .clientAuthenticationMethods(client.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue).collect(Collectors.toSet()))
                .authorizationGrantTypes(client.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue).collect(Collectors.toSet()))
                .redirectUris(client.getRedirectUris())
                .postLogoutRedirectUris(client.getPostLogoutRedirectUris())
                .scopes(client.getScopes())
                .build();
    }
}
