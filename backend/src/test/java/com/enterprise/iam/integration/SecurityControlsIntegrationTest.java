package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtTokenProvider;
import com.enterprise.iam.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = "app.rate-limit.enabled=true")
public class SecurityControlsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private com.enterprise.iam.security.RateLimitFilter rateLimitFilter;

    @Autowired
    private com.enterprise.iam.repository.SessionRepository sessionRepository;

    @org.springframework.beans.factory.annotation.Value("${jwt.secret:defaultSecretKeyThatIsAtLeast32BytesLongForHS256Algorithm}")
    private String jwtSecret;

    @org.springframework.beans.factory.annotation.Value("${jwt.issuer:enterprise-iam}")
    private String jwtIssuer;

    @org.springframework.beans.factory.annotation.Value("${jwt.audience:enterprise-iam-client}")
    private String jwtAudience;

    private UUID tenantId;
    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        rateLimitFilter.clearBuckets();
        
        Organization org = new Organization();
        org.setName("Security Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        testUser = new User();
        testUser.setOrganizationId(tenantId);
        testUser.setEmail("sec-user@example.com");
        testUser.setFirstName("Sec");
        testUser.setLastName("User");
        testUser.setStatus("ACTIVE");
        testUser.setPasswordHash(passwordEncoder.encode("SecureP@ssw0rd"));
        userRepository.save(testUser);

        validToken = jwtTokenProvider.generateToken(testUser.getId(), tenantId, "session-123", java.util.Collections.emptySet());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void testApiRateLimiting() throws Exception {
        // Send 101 requests rapidly to API
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/v1/health")
                    .header("X-Forwarded-For", "192.168.1.100"))
                    .andExpect(status().isOk());
        }
        
        // The 101st request should be rate-limited
        mockMvc.perform(get("/api/v1/health")
                .header("X-Forwarded-For", "192.168.1.100"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testLoginRateLimiting() throws Exception {
        String loginJson = "{\"email\":\"sec-user@example.com\",\"password\":\"WrongPassword\"}";
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "192.168.1.101")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", "192.168.1.101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void testJwtModification() throws Exception {
        // Modify the payload of a valid JWT slightly
        String[] parts = validToken.split("\\.");
        String modifiedPayload = parts[1] + "a";
        String tamperedToken = parts[0] + "." + modifiedPayload + "." + parts[2];

        mockMvc.perform(get("/api/v1/sessions")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWrongSignature() throws Exception {
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + "." + "invalid_signature_here";

        mockMvc.perform(get("/api/v1/sessions")
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    private String generateCustomToken(String issuer, String audience, long expirationOffsetMs) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(
                java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        java.util.Date now = new java.util.Date();
        java.util.Date expiryDate = new java.util.Date(now.getTime() + expirationOffsetMs);
        return io.jsonwebtoken.Jwts.builder()
                .subject(testUser.getId().toString())
                .claim("tenantId", tenantId.toString())
                .claim("email", testUser.getEmail())
                .issuer(issuer)
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    @Test
    void testExpiredToken() throws Exception {
        String expiredToken = generateCustomToken(jwtIssuer, jwtAudience, -10000); // 10 seconds in the past

        mockMvc.perform(get("/api/v1/sessions")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWrongIssuer() throws Exception {
        String wrongIssuerToken = generateCustomToken("wrong-issuer", jwtAudience, 900000);

        mockMvc.perform(get("/api/v1/sessions")
                .header("Authorization", "Bearer " + wrongIssuerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testWrongAudience() throws Exception {
        String wrongAudienceToken = generateCustomToken(jwtIssuer, "wrong-audience", 900000);

        mockMvc.perform(get("/api/v1/sessions")
                .header("Authorization", "Bearer " + wrongAudienceToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testBruteForceLockout() throws Exception {
        String loginJson = "{\"email\":\"sec-user@example.com\",\"password\":\"WrongPassword\"}";
        
        // 5 failed attempts
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .header("X-Tenant-ID", tenantId.toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson));
        }

        // 6th attempt should be blocked due to account lockout, even with correct password
        String correctLoginJson = "{\"email\":\"sec-user@example.com\",\"password\":\"SecureP@ssw0rd\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(correctLoginJson))
                .andExpect(status().isUnauthorized()); // LoginAttemptService throws SecurityException which maps to 401
    }

    @Test
    void testRefreshTokenReplay() throws Exception {
        // Setup initial session with refresh token
        com.enterprise.iam.domain.Session session = new com.enterprise.iam.domain.Session();
        session.setUserId(testUser.getId());
        session.setTenantId(tenantId);
        UUID tokenFamily = UUID.randomUUID();
        session.setTokenFamily(tokenFamily);
        String oldRefreshToken = UUID.randomUUID().toString();
        // hash it
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(oldRefreshToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String oldHash = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        session.setRefreshTokenHash(oldHash);
        session.setRevoked(true); // SIMULATE REPLAY: This token is already revoked!
        session.setExpiresAt(java.time.Instant.now().plus(java.time.Duration.ofDays(1)));
        sessionRepository.save(session);

        // Also create a "current" valid session in the same family to verify it gets revoked
        com.enterprise.iam.domain.Session currentSession = new com.enterprise.iam.domain.Session();
        currentSession.setUserId(testUser.getId());
        currentSession.setTenantId(tenantId);
        currentSession.setTokenFamily(tokenFamily);
        currentSession.setRefreshTokenHash("some_other_hash");
        currentSession.setRevoked(false);
        currentSession.setExpiresAt(java.time.Instant.now().plus(java.time.Duration.ofDays(1)));
        sessionRepository.save(currentSession);

        String refreshJson = "{\"refreshToken\":\"" + oldRefreshToken + "\"}";

        // Reusing the revoked refresh token
        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("X-Tenant-ID", tenantId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshJson))
                .andExpect(status().isUnauthorized());

        // Verify the entire token family was revoked
        com.enterprise.iam.domain.Session savedCurrentSession = sessionRepository.findById(currentSession.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(savedCurrentSession.isRevoked(), "Token family should be revoked upon replay");
    }

    @Test
    void testUnauthorizedTenantAccess() throws Exception {
        Organization tenantB = new Organization();
        tenantB.setName("Tenant B");
        tenantB = organizationRepository.save(tenantB);

        // testUser belongs to tenantId (Tenant A). Try to access Tenant B's endpoint using Tenant A's token.
        // E.g., getting an organization
        mockMvc.perform(get("/api/v1/organizations/" + tenantB.getId())
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDisabledUserAccess() throws Exception {
        testUser.setStatus("DISABLED");
        userRepository.save(testUser);

        mockMvc.perform(get("/api/v1/sessions")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized());
    }
}
