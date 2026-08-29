package com.enterprise.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreatePolicyRequest {
    @NotBlank
    private String name;
    
    @NotBlank
    private String effect;
    
    @NotEmpty
    private List<String> actions;
    
    @NotEmpty
    private List<String> resources;
    
    private String conditions;
    
    private int priority;
}
