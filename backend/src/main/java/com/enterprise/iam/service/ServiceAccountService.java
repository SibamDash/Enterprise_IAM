package com.enterprise.iam.service;

import com.enterprise.iam.domain.Role;
import com.enterprise.iam.domain.ServiceAccount;
import com.enterprise.iam.dto.serviceaccount.CreateServiceAccountRequest;
import com.enterprise.iam.dto.serviceaccount.ServiceAccountDto;
import com.enterprise.iam.dto.serviceaccount.UpdateServiceAccountRequest;
import com.enterprise.iam.repository.RoleRepository;
import com.enterprise.iam.repository.ServiceAccountRepository;
import com.enterprise.iam.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAccountService {

    private final ServiceAccountRepository serviceAccountRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<ServiceAccountDto> getAllServiceAccounts() {
        UUID organizationId = TenantContextHolder.getTenantId();
        return serviceAccountRepository.findAllByOrganizationId(organizationId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceAccountDto getServiceAccountById(String id) {
        UUID organizationId = TenantContextHolder.getTenantId();
        ServiceAccount account = serviceAccountRepository.findByIdAndOrganizationId(UUID.fromString(id), organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Service account not found"));
        return mapToDto(account);
    }

    @Transactional
    public ServiceAccountDto createServiceAccount(CreateServiceAccountRequest request) {
        UUID organizationId = TenantContextHolder.getTenantId();
        
        if (serviceAccountRepository.findByClientId(request.getClientId()).isPresent()) {
            throw new IllegalArgumentException("Service account for this client ID already exists");
        }

        ServiceAccount account = new ServiceAccount();
        account.setClientId(request.getClientId());
        account.setOrganizationId(organizationId);
        account.setName(request.getName());

        Set<Role> roles = resolveRoles(request.getRoleIds(), organizationId);
        account.setRoles(roles);

        account = serviceAccountRepository.save(account);
        return mapToDto(account);
    }

    @Transactional
    public ServiceAccountDto updateServiceAccount(String id, UpdateServiceAccountRequest request) {
        UUID organizationId = TenantContextHolder.getTenantId();
        ServiceAccount account = serviceAccountRepository.findByIdAndOrganizationId(UUID.fromString(id), organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Service account not found"));

        account.setName(request.getName());
        
        Set<Role> roles = resolveRoles(request.getRoleIds(), organizationId);
        account.setRoles(roles);

        account = serviceAccountRepository.save(account);
        return mapToDto(account);
    }

    @Transactional
    public void deleteServiceAccount(String id) {
        UUID organizationId = TenantContextHolder.getTenantId();
        ServiceAccount account = serviceAccountRepository.findByIdAndOrganizationId(UUID.fromString(id), organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Service account not found"));
        
        serviceAccountRepository.delete(account);
    }

    private Set<Role> resolveRoles(Set<String> roleIds, UUID organizationId) {
        Set<Role> roles = new HashSet<>();
        for (String roleIdStr : roleIds) {
            UUID roleId = UUID.fromString(roleIdStr);
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleIdStr));
            if (!role.getOrganizationId().equals(organizationId)) {
                throw new IllegalArgumentException("Role does not belong to your organization: " + roleIdStr);
            }
            roles.add(role);
        }
        return roles;
    }

    private ServiceAccountDto mapToDto(ServiceAccount account) {
        ServiceAccountDto dto = new ServiceAccountDto();
        dto.setId(account.getId().toString());
        dto.setClientId(account.getClientId());
        dto.setName(account.getName());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setUpdatedAt(account.getUpdatedAt());
        dto.setRoles(account.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
        return dto;
    }
}
