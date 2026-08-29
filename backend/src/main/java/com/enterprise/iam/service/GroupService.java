package com.enterprise.iam.service;

import com.enterprise.iam.domain.Group;
import com.enterprise.iam.dto.CreateGroupRequest;
import com.enterprise.iam.dto.GroupDto;
import com.enterprise.iam.repository.GroupRepository;
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
public class GroupService {

    private final GroupRepository groupRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public GroupDto createGroup(CreateGroupRequest request) {
        UUID tenantId = getTenantId();

        if (groupRepository.existsByNameAndOrganizationId(request.getName(), tenantId)) {
            throw new IllegalArgumentException("Group with this name already exists in the organization");
        }

        Group group = new Group();
        group.setOrganizationId(tenantId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        
        group = groupRepository.save(group);
        return mapToDto(group);
    }

    @Transactional(readOnly = true)
    public GroupDto getGroup(UUID id) {
        return groupRepository.findByIdAndOrganizationId(id, getTenantId())
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("Group not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Page<GroupDto> listGroups(Pageable pageable) {
        return groupRepository.findAllByOrganizationId(getTenantId(), pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deleteGroup(UUID id) {
        Group group = groupRepository.findByIdAndOrganizationId(id, getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Group not found or access denied"));
        groupRepository.delete(group);
    }

    @Transactional
    public GroupDto assignRoles(UUID id, java.util.Set<UUID> roleIds) {
        Group group = groupRepository.findByIdAndOrganizationId(id, getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Group not found or access denied"));
        
        java.util.List<com.enterprise.iam.domain.Role> roles = roleRepository.findAllById(roleIds);
        // Verify all roles belong to the same tenant
        UUID tenantId = getTenantId();
        for (com.enterprise.iam.domain.Role role : roles) {
            if (!role.getOrganizationId().equals(tenantId)) {
                throw new SecurityException("Cannot assign roles from a different organization");
            }
        }
        
        group.getRoles().clear();
        group.getRoles().addAll(roles);
        group = groupRepository.save(group);
        return mapToDto(group);
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available for this operation");
        }
        return tenantId;
    }

    private GroupDto mapToDto(Group group) {
        GroupDto dto = new GroupDto();
        dto.setId(group.getId());
        dto.setOrganizationId(group.getOrganizationId());
        dto.setName(group.getName());
        dto.setDescription(group.getDescription());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        
        if (group.getRoles() != null) {
            java.util.Set<UUID> roleIds = new java.util.HashSet<>();
            for (com.enterprise.iam.domain.Role role : group.getRoles()) {
                roleIds.add(role.getId());
            }
            dto.setRoleIds(roleIds);
        } else {
            dto.setRoleIds(new java.util.HashSet<>());
        }
        
        return dto;
    }
}
