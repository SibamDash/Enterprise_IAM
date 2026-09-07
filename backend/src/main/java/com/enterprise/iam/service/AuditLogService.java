package com.enterprise.iam.service;

import com.enterprise.iam.domain.AuditLog;
import com.enterprise.iam.dto.AuditLogDto;
import com.enterprise.iam.repository.AuditLogRepository;
import com.enterprise.iam.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> listAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrganizationIdOrderByCreatedAtDesc(getTenantId(), pageable)
                .map(this::mapToDto);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLogDto> listAuditLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(getTenantId(), userId, pageable)
                .map(this::mapToDto);
    }
    
    @Transactional(readOnly = true)
    public Page<AuditLogDto> listAuditLogsByEventType(String eventType, Pageable pageable) {
        return auditLogRepository.findAllByOrganizationIdAndEventTypeOrderByCreatedAtDesc(getTenantId(), eventType, pageable)
                .map(this::mapToDto);
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available for this operation");
        }
        return tenantId;
    }

    private AuditLogDto mapToDto(AuditLog auditLog) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(auditLog.getId());
        dto.setOrganizationId(auditLog.getOrganizationId());
        dto.setUserId(auditLog.getUserId());
        dto.setEventType(auditLog.getEventType());
        dto.setIpAddress(auditLog.getIpAddress());
        dto.setUserAgent(auditLog.getUserAgent());
        dto.setDetails(auditLog.getDetails());
        dto.setCreatedAt(auditLog.getCreatedAt());
        return dto;
    }
}
