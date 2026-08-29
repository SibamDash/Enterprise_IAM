package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class AssignRolesRequest {
    @NotNull
    private Set<UUID> roleIds;
}
