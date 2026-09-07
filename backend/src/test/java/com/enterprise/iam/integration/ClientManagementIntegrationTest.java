package com.enterprise.iam.integration;

import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.dto.client.ClientDto;
import com.enterprise.iam.dto.client.ClientSecretResponse;
import com.enterprise.iam.dto.client.CreateClientRequest;
import com.enterprise.iam.dto.client.UpdateClientRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ClientManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Assume tenant seed and admin user exists from migrations
        // We log in to get the JWT token for an admin
        String tenantId = organizationRepository.findAll().get(0).getId().toString();

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("admin@acme.com");
        loginReq.setPassword("SecurePassword123!");
        
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        adminToken = objectMapper.readTree(responseBody).get("accessToken").asText();
    }

    @Test
    void testClientCrudCycle() throws Exception {
        // 1. Create a client
        CreateClientRequest createReq = CreateClientRequest.builder()
                .clientId("test-client-id")
                .clientName("Test Application")
                .clientAuthenticationMethods(Set.of("client_secret_basic"))
                .authorizationGrantTypes(Set.of("authorization_code"))
                .redirectUris(Set.of("http://localhost:8080/callback"))
                .scopes(Set.of("openid", "profile"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/clients")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ClientSecretResponse secretResponse = objectMapper.readValue(createResult.getResponse().getContentAsString(), ClientSecretResponse.class);
        assertNotNull(secretResponse.getId());
        assertEquals("test-client-id", secretResponse.getClientId());
        assertNotNull(secretResponse.getClientSecret()); // We only get it here!

        String createdId = secretResponse.getId();

        // 2. Get the client and verify secret is NOT returned
        MvcResult getResult = mockMvc.perform(get("/api/v1/clients/" + createdId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        ClientDto getDto = objectMapper.readValue(getResult.getResponse().getContentAsString(), ClientDto.class);
        assertEquals("test-client-id", getDto.getClientId());
        assertEquals("Test Application", getDto.getClientName());
        assertTrue(getDto.getRedirectUris().contains("http://localhost:8080/callback"));
        assertFalse(getResult.getResponse().getContentAsString().contains(secretResponse.getClientSecret()));

        // 3. Update the client
        UpdateClientRequest updateReq = UpdateClientRequest.builder()
                .clientName("Updated Application")
                .clientAuthenticationMethods(Set.of("client_secret_basic"))
                .authorizationGrantTypes(Set.of("authorization_code", "client_credentials"))
                .redirectUris(Set.of("http://localhost:8080/updated-callback"))
                .scopes(Set.of("openid"))
                .build();

        MvcResult updateResult = mockMvc.perform(put("/api/v1/clients/" + createdId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andReturn();

        ClientDto updatedDto = objectMapper.readValue(updateResult.getResponse().getContentAsString(), ClientDto.class);
        assertEquals("Updated Application", updatedDto.getClientName());
        assertTrue(updatedDto.getRedirectUris().contains("http://localhost:8080/updated-callback"));

        // 4. Rotate secret
        MvcResult rotateResult = mockMvc.perform(post("/api/v1/clients/" + createdId + "/rotate-secret")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        ClientSecretResponse rotateResponse = objectMapper.readValue(rotateResult.getResponse().getContentAsString(), ClientSecretResponse.class);
        assertEquals(createdId, rotateResponse.getId());
        assertNotNull(rotateResponse.getClientSecret());
        assertNotEquals(secretResponse.getClientSecret(), rotateResponse.getClientSecret());

        // 5. List all clients to ensure it's in the list
        MvcResult listResult = mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<ClientDto> listDtos = objectMapper.readValue(listResult.getResponse().getContentAsString(), new TypeReference<List<ClientDto>>() {});
        assertTrue(listDtos.stream().anyMatch(c -> c.getId().equals(createdId)));

        // 6. Delete the client
        mockMvc.perform(delete("/api/v1/clients/" + createdId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 7. Verify deletion
        mockMvc.perform(get("/api/v1/clients/" + createdId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/clients"))
                .andExpect(status().isUnauthorized());
    }
}
