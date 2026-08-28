package com.enterprise.iam.service;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.CreateUserRequest;
import com.enterprise.iam.dto.UserDto;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createUser_ShouldSaveUser_WhenValidRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmailAndOrganizationId(request.getEmail(), tenantId)).thenReturn(false);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setOrganizationId(tenantId);
        savedUser.setEmail(request.getEmail());
        savedUser.setFirstName(request.getFirstName());
        savedUser.setLastName(request.getLastName());
        savedUser.setStatus("ACTIVE");

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserDto result = userService.createUser(request);

        assertNotNull(result);
        assertEquals(request.getEmail(), result.getEmail());
        assertEquals(tenantId, result.getOrganizationId());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowException_WhenUserExists() {
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        
        when(userRepository.existsByEmailAndOrganizationId(request.getEmail(), tenantId)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any(User.class));
    }
}
