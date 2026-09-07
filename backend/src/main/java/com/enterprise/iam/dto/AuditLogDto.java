package com.enterprise.iam.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class AuditLogDto {
    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String eventType;
    private String ipAddress;
    private String userAgent;
    private String details;
    private OffsetDateTime createdAt;
}
