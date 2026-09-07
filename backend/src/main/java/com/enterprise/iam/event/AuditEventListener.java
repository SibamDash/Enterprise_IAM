package com.enterprise.iam.event;

import com.enterprise.iam.domain.AuditLog;
import com.enterprise.iam.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @Async
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOrganizationId(event.getOrganizationId());
        auditLog.setUserId(event.getUserId());
        auditLog.setEventType(event.getEventType());
        auditLog.setIpAddress(event.getIpAddress());
        auditLog.setUserAgent(event.getUserAgent());
        auditLog.setDetails(event.getDetails());

        auditLogRepository.save(auditLog);
    }
}
