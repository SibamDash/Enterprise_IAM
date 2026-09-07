package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.Role;
import com.enterprise.iam.domain.ServiceAccount;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.ServiceAccountRepository;
import com.enterprise.iam.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MachineToMachineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private ServiceAccountRepository serviceAccountRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ObjectMapper objectMapper;

    private Organization org;
    private Role readRole;
    private String clientId = "reporting-service-123";
    private String clientSecret = "super-secret-password";

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
        serviceAccountRepository.deleteAll();
        
        // Find existing organization from seeder or create one
        List<Organization> orgs = organizationRepository.findAll();
        if (orgs.isEmpty()) {
            org = new Organization();
            org.setName("M2M Test Org");
            org.setStatus("ACTIVE");
            org = organizationRepository.save(org);
        } else {
            org = orgs.get(0);
        }

        readRole = new Role();
        readRole.setOrganizationId(org.getId());
        readRole.setName("USER_READER");
        readRole.setDescription("Can read users");
        readRole.setPermissions(new HashSet<>(Arrays.asList("USER_READ")));
        readRole = roleRepository.save(readRole);

        // 1. Create OAuth2 Client
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
        registeredClientRepository.save(client);

        // 2. Create Service Account mapping to Client
        ServiceAccount sa = new ServiceAccount();
        sa.setClientId(clientId);
        sa.setOrganizationId(org.getId());
        sa.setName("Reporting Service");
        sa.getRoles().add(readRole);
        serviceAccountRepository.save(sa);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        serviceAccountRepository.deleteAll();
        // Since RegisteredClientRepository does not have a delete method built-in,
        // it persists across tests. For test isolation, we ideally clean it up.
    }

    @Test
    void testClientCredentialsFlowAndResourceAccess() throws Exception {
        // Step 1: Obtain Access Token using client_credentials grant
        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        MvcResult tokenResult = mockMvc.perform(post("/oauth2/token")
                .header("Authorization", basicAuthHeader)
                .param("grant_type", "client_credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andReturn();

        String responseBody = tokenResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).get("access_token").asText();

        // Step 2: Use Access Token to call our API
        // Our ServiceAccount is linked to org and has USER_READ permission.
        // It should successfully call GET /api/v1/users (which requires USER_READ).
        mockMvc.perform(get("/api/v1/users")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
                
        // Step 3: Attempt to call an API it doesn't have permission for
        // e.g. GET /api/v1/clients requires CLIENT_READ
        mockMvc.perform(get("/api/v1/clients")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}
