package com.enterprise.iam.dto.client;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientRequest {
    
    @NotBlank
    private String clientName;

    @NotEmpty
    private Set<String> clientAuthenticationMethods;

    @NotEmpty
    private Set<String> authorizationGrantTypes;

    private Set<String> redirectUris;
    
    private Set<String> postLogoutRedirectUris;
    
    private Set<String> scopes;
}
