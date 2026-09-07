package com.enterprise.iam.dto.serviceaccount;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Set;

@Data
public class ServiceAccountDto {
    private String id;
    private String clientId;
    private String name;
    private Set<String> roles;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
