package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AssignGroupsRequest {
    @NotNull
    private Set<UUID> groupIds;
}
