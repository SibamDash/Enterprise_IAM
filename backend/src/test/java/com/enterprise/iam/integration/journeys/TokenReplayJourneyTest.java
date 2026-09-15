package com.enterprise.iam.integration.journeys;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.SessionRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtTokenProvider;
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
public class TokenReplayJourneyTest {

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
        org.setName("Token Replay Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        testUser = new User();
        testUser.setOrganizationId(tenantId);
        testUser.setEmail("replay@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("Secure123!"));
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
    void executeJourneyE_TokenReplayDetection() throws Exception {
        // 1. Initial Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("replay@example.com");
        loginReq.setPassword("Secure123!");
        
        var authResponse = authService.authenticate(loginReq, "Agent", "127.0.0.1");
        String originalRefreshToken = authResponse.getRefreshToken();
        
        // 2. Legitimate Refresh
        var refreshReq = new com.enterprise.iam.dto.RefreshRequest();
        refreshReq.setRefreshToken(originalRefreshToken);
        
        String refreshRespJson = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
                
        // 3. Token Replay (Attempting to use the already consumed originalRefreshToken)
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
                
        // 4. Verify token family was revoked
        assertThat(sessionRepository.findByUserIdAndTenantIdAndRevokedFalse(testUser.getId(), tenantId)).isEmpty();
    }
}
