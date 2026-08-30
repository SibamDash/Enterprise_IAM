package com.enterprise.iam.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MfaEnableResponse {
    private List<String> recoveryCodes;
}
