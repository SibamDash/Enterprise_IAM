package com.enterprise.iam.integration;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.util.Set;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OidcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setOrganizationId(tenantId);
        testUser.setEmail("oidcuser@example.com");
        testUser.setFirstName("Oidc");
        testUser.setLastName("User");
        testUser.setStatus("ACTIVE");
        testUser.setPasswordHash("hashedpassword");

        testUser = userRepository.save(testUser);
    }

    @Test
    void discoveryEndpointReturnsValidConfiguration() throws Exception {
        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").exists())
                .andExpect(jsonPath("$.authorization_endpoint").exists())
                .andExpect(jsonPath("$.jwks_uri").exists())
                .andExpect(jsonPath("$.subject_types_supported").exists())
                .andExpect(jsonPath("$.response_types_supported").exists())
                .andExpect(jsonPath("$.scopes_supported").exists());
    }

    @Test
    void jwksEndpointReturnsKeys() throws Exception {
        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    @Test
    void customizerAddsClaimsToIdToken() {
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();
        
        JwtEncodingContext context = JwtEncodingContext.with(JwsHeader.with(SignatureAlgorithm.RS256), claimsBuilder)
                .tokenType(new OAuth2TokenType(OidcParameterNames.ID_TOKEN))
                .principal(new UsernamePasswordAuthenticationToken(testUser.getId().toString(), null))
                .authorizedScopes(Set.of(OidcScopes.OPENID))
                .build();

        jwtTokenCustomizer.customize(context);

        JwtClaimsSet claims = claimsBuilder.build();
        assertEquals("oidcuser@example.com", claims.getClaim("email"));
        assertEquals("Oidc User", claims.getClaim("name"));
        assertEquals("oidcuser@example.com", claims.getClaim("preferred_username"));
        assertEquals(tenantId.toString(), claims.getClaim("tenantId"));
    }
}
