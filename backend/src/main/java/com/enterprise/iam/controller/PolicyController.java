package com.enterprise.iam.controller;

import com.enterprise.iam.dto.CreatePolicyRequest;
import com.enterprise.iam.dto.PolicyDto;
import com.enterprise.iam.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PreAuthorize("hasAuthority('POLICY_CREATE')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PolicyDto createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        return policyService.createPolicy(request);
    }

    @PreAuthorize("hasAuthority('POLICY_READ')")
    @GetMapping("/{id}")
    public PolicyDto getPolicy(@PathVariable UUID id) {
        return policyService.getPolicy(id);
    }

    @PreAuthorize("hasAuthority('POLICY_READ')")
    @GetMapping
    public Page<PolicyDto> listPolicies(Pageable pageable) {
        return policyService.listPolicies(pageable);
    }

    @PreAuthorize("hasAuthority('POLICY_DELETE')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePolicy(@PathVariable UUID id) {
        policyService.deletePolicy(id);
    }
}
