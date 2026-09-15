package com.enterprise.iam.integration.journeys;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.SessionRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SsoJourneyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthService authService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID tenantId;
    private User testUser;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("SSO Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        testUser = new User();
        testUser.setOrganizationId(tenantId);
        testUser.setEmail("sso@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser.setStatus("ACTIVE");
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void executeJourneyD_SsoSessionRecognition() throws Exception {
        // App A login
        LoginRequest loginReqA = new LoginRequest();
        loginReqA.setEmail("sso@example.com");
        loginReqA.setPassword("Password123!");
        
        var authResponseA = authService.authenticate(loginReqA, "App A", "127.0.0.1");
        assertThat(authResponseA.getAccessToken()).isNotEmpty();
        
        // App B attempts to use the same refresh token (simulating a shared SSO cookie or session store)
        var refreshReq = new com.enterprise.iam.dto.RefreshRequest();
        refreshReq.setRefreshToken(authResponseA.getRefreshToken());
        
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());
    }
}
