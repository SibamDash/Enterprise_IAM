package com.enterprise.iam.integration;

import com.enterprise.iam.domain.Organization;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.domain.AuditLog;
import com.enterprise.iam.repository.AuditLogRepository;
import com.enterprise.iam.repository.OrganizationRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.TenantContextHolder;
import com.enterprise.iam.service.DashboardService;
import com.enterprise.iam.dto.DashboardMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class DashboardIntegrationTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Organization org;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        org = new Organization();
        org.setName("Dashboard Test Org");
        org.setStatus("ACTIVE");
        org = organizationRepository.save(org);

        TenantContextHolder.setTenantId(org.getId());
    }

    @Test
    void shouldReturnCorrectMetrics() {
        // Add active user
        User activeUser = new User();
        activeUser.setOrganizationId(org.getId());
        activeUser.setEmail("active@example.com");
        activeUser.setStatus("ACTIVE");
        userRepository.save(activeUser);

        // Add locked user
        User lockedUser = new User();
        lockedUser.setOrganizationId(org.getId());
        lockedUser.setEmail("locked@example.com");
        lockedUser.setStatus("LOCKED");
        userRepository.save(lockedUser);

        // Add failed login audit log
        AuditLog auditLog = new AuditLog();
        auditLog.setOrganizationId(org.getId());
        auditLog.setEventType("USER_LOGIN_FAILED");
        auditLog.setIpAddress("127.0.0.1");
        auditLogRepository.save(auditLog);

        DashboardMetricsDto metrics = dashboardService.getMetrics();

        assertThat(metrics.getTotalUsers()).isEqualTo(2);
        assertThat(metrics.getActiveUsers()).isEqualTo(1);
        assertThat(metrics.getLockedAccounts()).isEqualTo(1);
        assertThat(metrics.getFailedLogins()).isEqualTo(1);
    }
}
