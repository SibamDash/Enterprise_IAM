package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.TenantContextHolder;
import com.enterprise.iam.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("Auth Test Org");
        org = organizationRepository.save(org);
        tenantId = org.getId();

        User user = new User();
        user.setOrganizationId(tenantId);
        user.setEmail("auth@example.com");
        user.setFirstName("Auth");
        user.setLastName("User");
        user.setStatus("ACTIVE");
        user.setPasswordHash(passwordEncoder.encode("SecureP@ssw0rd"));
        userRepository.save(user);

        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
        
        // Clean Redis
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void testSuccessfulLogin() {
        LoginRequest req = new LoginRequest();
        req.setEmail("auth@example.com");
        req.setPassword("SecureP@ssw0rd");
        
        // Should not throw
        authService.authenticate(req, "IntegrationTestAgent", "127.0.0.1");
    }

    @Test
    void testFailedLoginAndLockout() {
        LoginRequest req = new LoginRequest();
        req.setEmail("auth@example.com");
        req.setPassword("wrong");

        // Fail 5 times
        for (int i = 0; i < 5; i++) {
            assertThrows(SecurityException.class, () -> authService.authenticate(req, "IntegrationTestAgent", "127.0.0.1"));
        }

        // 6th time should be locked, even if password is correct
        req.setPassword("SecureP@ssw0rd");
        SecurityException e = assertThrows(SecurityException.class, () -> authService.authenticate(req, "IntegrationTestAgent", "127.0.0.1"));
        assert e.getMessage().contains("locked");
    }
}
