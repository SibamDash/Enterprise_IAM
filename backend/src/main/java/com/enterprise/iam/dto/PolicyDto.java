package com.enterprise.iam.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class PolicyDto {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String effect;
    private List<String> actions;
    private List<String> resources;
    private String conditions;
    private int priority;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
