package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizationRequest {
    @NotBlank(message = "Organization name is required")
    private String name;
}
