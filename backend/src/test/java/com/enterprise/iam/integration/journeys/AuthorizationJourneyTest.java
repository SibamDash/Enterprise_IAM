package com.enterprise.iam.integration.journeys;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthorizationJourneyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private UUID tenantId;
    private String userWithAccess;
    private String userWithoutAccess;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("Authz Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        userWithAccess = jwtTokenProvider.generateToken(
                UUID.randomUUID(), 
                tenantId, 
                "authorized@example.com", 
                Set.of("CLIENT_READ")
        );
        
        userWithoutAccess = jwtTokenProvider.generateToken(
                UUID.randomUUID(), 
                tenantId, 
                "unauthorized@example.com", 
                Set.of("OTHER_PERMISSION")
        );
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void executeJourneyB_AuthorizedAccess() throws Exception {
        // User with CLIENT_READ attempts to read clients
        mockMvc.perform(get("/api/v1/clients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userWithAccess))
                .andExpect(status().isOk());
    }

    @Test
    void executeJourneyC_UnauthorizedAccess() throws Exception {
        // User without CLIENT_READ attempts to read clients
        mockMvc.perform(get("/api/v1/clients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userWithoutAccess))
                .andExpect(status().isForbidden());
    }
}
