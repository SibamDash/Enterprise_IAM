package com.enterprise.iam.service;

import com.enterprise.iam.domain.Role;
import com.enterprise.iam.dto.CreateRoleRequest;
import com.enterprise.iam.dto.RoleDto;
import com.enterprise.iam.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        UUID tenantId = getTenantId();

        if (roleRepository.existsByNameAndOrganizationId(request.getName(), tenantId)) {
            throw new IllegalArgumentException("Role with this name already exists in the organization");
        }

        Role role = new Role();
        role.setOrganizationId(tenantId);
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        
        role = roleRepository.save(role);
        return mapToDto(role);
    }

    @Transactional(readOnly = true)
    public RoleDto getRole(UUID id) {
        return roleRepository.findByIdAndOrganizationId(id, getTenantId())
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Role not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Page<RoleDto> listRoles(Pageable pageable) {
        return roleRepository.findAllByOrganizationId(getTenantId(), pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deleteRole(UUID id) {
        Role role = roleRepository.findByIdAndOrganizationId(id, getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Role not found or access denied"));
        roleRepository.delete(role);
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available for this operation");
        }
        return tenantId;
    }

    private RoleDto mapToDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setOrganizationId(role.getOrganizationId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }
}
