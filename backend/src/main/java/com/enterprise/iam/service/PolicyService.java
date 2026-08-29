package com.enterprise.iam.service;

import com.enterprise.iam.domain.Policy;
import com.enterprise.iam.dto.CreatePolicyRequest;
import com.enterprise.iam.dto.PolicyDto;
import com.enterprise.iam.repository.PolicyRepository;
import com.enterprise.iam.security.TenantContextHolder;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;

    @Transactional
    public PolicyDto createPolicy(CreatePolicyRequest request) {
        UUID tenantId = getTenantId();

        if (policyRepository.existsByNameAndOrganizationId(request.getName(), tenantId)) {
            throw new IllegalArgumentException("Policy with this name already exists in the organization");
        }

        Policy policy = new Policy();
        policy.setOrganizationId(tenantId);
        policy.setName(request.getName());
        policy.setEffect(request.getEffect());
        policy.setActions(request.getActions());
        policy.setResources(request.getResources());
        policy.setConditions(request.getConditions());
        policy.setPriority(request.getPriority());

        policy = policyRepository.save(policy);
        return mapToDto(policy);
    }

    @Transactional(readOnly = true)
    public PolicyDto getPolicy(UUID id) {
        return policyRepository.findByIdAndOrganizationId(id, getTenantId())
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Page<PolicyDto> listPolicies(Pageable pageable) {
        return policyRepository.findAllByOrganizationId(getTenantId(), pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deletePolicy(UUID id) {
        Policy policy = policyRepository.findByIdAndOrganizationId(id, getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Policy not found or access denied"));
        policyRepository.delete(policy);
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available for this operation");
        }
        return tenantId;
    }

    private PolicyDto mapToDto(Policy policy) {
        PolicyDto dto = new PolicyDto();
        dto.setId(policy.getId());
        dto.setOrganizationId(policy.getOrganizationId());
        dto.setName(policy.getName());
        dto.setEffect(policy.getEffect());
        dto.setActions(policy.getActions());
        dto.setResources(policy.getResources());
        dto.setConditions(policy.getConditions());
        dto.setPriority(policy.getPriority());
        dto.setCreatedAt(policy.getCreatedAt());
        dto.setUpdatedAt(policy.getUpdatedAt());
        return dto;
    }
}
