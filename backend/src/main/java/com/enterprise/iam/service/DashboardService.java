package com.enterprise.iam.service;

import com.enterprise.iam.dto.DashboardMetricsDto;
import com.enterprise.iam.repository.AuditLogRepository;
import com.enterprise.iam.repository.SessionRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public DashboardMetricsDto getMetrics() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available for this operation");
        }

        long totalUsers = userRepository.countByOrganizationId(tenantId);
        long activeUsers = userRepository.countByOrganizationIdAndStatus(tenantId, "ACTIVE");
        long lockedAccounts = userRepository.countByOrganizationIdAndStatus(tenantId, "LOCKED");
        long activeSessions = sessionRepository.countByTenantIdAndRevokedFalse(tenantId);
        
        // Clients are currently platform-wide in the schema, but we can query total registered
        // If they become tenant-isolated, we'd add tenantId to the query.
        Long clientsCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oauth2_registered_client", Long.class);
        long registeredApplications = clientsCount != null ? clientsCount : 0L;

        long failedLogins = auditLogRepository.countByOrganizationIdAndEventType(tenantId, "USER_LOGIN_FAILED");
        long securityEvents = auditLogRepository.countByOrganizationIdAndEventType(tenantId, "SECURITY_EVENT");

        return DashboardMetricsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lockedAccounts(lockedAccounts)
                .activeSessions(activeSessions)
                .registeredApplications(registeredApplications)
                .failedLogins(failedLogins)
                .recentSecurityEvents(securityEvents)
                .build();
    }
}
