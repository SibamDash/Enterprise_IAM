package com.enterprise.iam.dto.serviceaccount;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Set;

@Data
public class CreateServiceAccountRequest {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Name is required")
    private String name;

    @NotNull(message = "Roles are required")
    private Set<String> roleIds;
}
