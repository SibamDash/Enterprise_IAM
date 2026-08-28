package com.enterprise.iam.service;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.LoginAttemptService;
import com.enterprise.iam.security.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthService authService;

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
    void testAuthenticate_Success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        User user = new User();
        user.setEmail("test@example.com");
        user.setStatus("ACTIVE");
        user.setPasswordHash("hashed_password");
        user.setOrganizationId(tenantId);

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmailAndOrganizationId(request.getEmail(), tenantId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed_password")).thenReturn(true);

        authService.authenticate(request);

        verify(loginAttemptService).loginSucceeded(anyString());
        verify(loginAttemptService, never()).loginFailed(anyString());
    }

    @Test
    void testAuthenticate_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmailAndOrganizationId(request.getEmail(), tenantId)).thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () -> authService.authenticate(request));

        verify(loginAttemptService).loginFailed(anyString());
    }

    @Test
    void testAuthenticate_AccountLocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password");

        when(loginAttemptService.isBlocked(anyString())).thenReturn(true);

        assertThrows(SecurityException.class, () -> authService.authenticate(request));
    }
}
