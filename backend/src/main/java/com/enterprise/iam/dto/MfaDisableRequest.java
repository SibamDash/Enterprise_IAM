package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaDisableRequest {
    @NotBlank
    private String code;
}
