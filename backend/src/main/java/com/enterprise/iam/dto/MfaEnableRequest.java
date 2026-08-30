package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MfaEnableRequest {
    @NotBlank
    private String secret;

    @NotBlank
    private String code;
}
