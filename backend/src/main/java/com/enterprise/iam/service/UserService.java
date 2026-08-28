package com.enterprise.iam.service;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.CreateUserRequest;
import com.enterprise.iam.dto.UserDto;
import com.enterprise.iam.repository.UserRepository;
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
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        UUID tenantId = getTenantId();

        if (userRepository.existsByEmailAndOrganizationId(request.getEmail(), tenantId)) {
            throw new IllegalArgumentException("User with this email already exists in the organization");
        }

        User user = new User();
        user.setOrganizationId(tenantId);
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        
        user = userRepository.save(user);
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(UUID id) {
        return userRepository.findByIdAndOrganizationId(id, getTenantId())
                .map(this::mapToDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found or access denied"));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> listUsers(Pageable pageable) {
        return userRepository.findAllByOrganizationId(getTenantId(), pageable)
                .map(this::mapToDto);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        User user = userRepository.findByIdAndOrganizationId(id, getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("User not found or access denied"));
        user.setStatus("INACTIVE");
        userRepository.save(user);
    }

    private UUID getTenantId() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant context available for this operation");
        }
        return tenantId;
    }

    private UserDto mapToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setOrganizationId(user.getOrganizationId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
