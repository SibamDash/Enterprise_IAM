package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.AuditLogRepository;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.TenantContextHolder;
import com.enterprise.iam.service.AuthService;
import com.enterprise.iam.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class AuditLogIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Organization org;
    private User testUser;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        org = new Organization();
        org.setName("Audit Test Org");
        org.setStatus("ACTIVE");
        org = organizationRepository.save(org);

        testUser = new User();
        testUser.setOrganizationId(org.getId());
        testUser.setEmail("audit.test@example.com");
        testUser.setFirstName("Audit");
        testUser.setLastName("Test");
        testUser.setPasswordHash(passwordEncoder.encode("Password123!"));
        testUser.setStatus("ACTIVE");
        testUser = userRepository.save(testUser);

        TenantContextHolder.setTenantId(org.getId());
    }

    @Test
    void shouldCreateAuditLogOnFailedLogin() throws InterruptedException {
        LoginRequest request = new LoginRequest();
        request.setEmail("audit.test@example.com");
        request.setPassword("WrongPassword!");

        assertThrows(SecurityException.class, () -> 
            authService.authenticate(request, "Test Agent", "127.0.0.1")
        );

        // Wait for async event to be processed
        TimeUnit.MILLISECONDS.sleep(500);

        assertThat(auditLogRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().get(0).getEventType()).isEqualTo("USER_LOGIN_FAILED");
        assertThat(auditLogRepository.findAll().get(0).getIpAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    void shouldCreateAuditLogOnSuccessfulLogin() throws InterruptedException {
        LoginRequest request = new LoginRequest();
        request.setEmail("audit.test@example.com");
        request.setPassword("Password123!");

        authService.authenticate(request, "Test Agent", "127.0.0.1");

        // Wait for async event to be processed
        TimeUnit.MILLISECONDS.sleep(500);

        assertThat(auditLogRepository.findAll()).hasSize(1);
        assertThat(auditLogRepository.findAll().get(0).getEventType()).isEqualTo("USER_LOGIN");
        assertThat(auditLogRepository.findAll().get(0).getUserId()).isEqualTo(testUser.getId());
    }
}
