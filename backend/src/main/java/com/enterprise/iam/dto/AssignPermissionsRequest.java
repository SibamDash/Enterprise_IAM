package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class AssignPermissionsRequest {
    @NotNull
    private Set<String> permissions;
}
