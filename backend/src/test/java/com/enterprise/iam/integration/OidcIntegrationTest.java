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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OidcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
}
