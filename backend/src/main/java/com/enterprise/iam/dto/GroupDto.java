package com.enterprise.iam.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class GroupDto {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private java.util.Set<UUID> roleIds;
}
