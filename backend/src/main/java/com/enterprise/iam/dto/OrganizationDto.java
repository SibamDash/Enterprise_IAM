package com.enterprise.iam.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class OrganizationDto {
    private UUID id;
    private String name;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
