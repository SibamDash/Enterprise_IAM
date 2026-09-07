package com.enterprise.iam.controller;

import com.enterprise.iam.dto.AuditLogDto;
import com.enterprise.iam.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public ResponseEntity<Page<AuditLogDto>> listAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String eventType,
            Pageable pageable) {
        
        if (userId != null) {
            return ResponseEntity.ok(auditLogService.listAuditLogsByUser(userId, pageable));
        } else if (eventType != null) {
            return ResponseEntity.ok(auditLogService.listAuditLogsByEventType(eventType, pageable));
        }
        
        return ResponseEntity.ok(auditLogService.listAuditLogs(pageable));
    }
}
