package com.enterprise.iam.event;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AuditEvent {
    private final UUID organizationId;
    private final UUID userId;
    private final String eventType;
    private final String ipAddress;
    private final String userAgent;
    private final String details; // JSON format
}
