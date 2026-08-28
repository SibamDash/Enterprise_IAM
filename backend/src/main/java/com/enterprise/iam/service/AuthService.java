package com.enterprise.iam.service;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.ChangePasswordRequest;
import com.enterprise.iam.dto.ForgotPasswordRequest;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.dto.ResetPasswordRequest;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.LoginAttemptService;
import com.enterprise.iam.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final StringRedisTemplate redisTemplate;

    private static final String RESET_TOKEN_PREFIX = "reset_token:";
    private static final Duration RESET_TOKEN_DURATION = Duration.ofMinutes(30);

    @Transactional(readOnly = true)
    public void authenticate(LoginRequest request) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is required");
        }

        String attemptKey = tenantId.toString() + ":" + request.getEmail();
        
        if (loginAttemptService.isBlocked(attemptKey)) {
            throw new SecurityException("Account is locked due to too many failed attempts");
        }

        Optional<User> userOpt = userRepository.findByEmailAndOrganizationId(request.getEmail(), tenantId);
        
        if (userOpt.isEmpty()) {
            loginAttemptService.loginFailed(attemptKey);
            throw new SecurityException("Invalid email or password");
        }
        
        User user = userOpt.get();
        
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new SecurityException("Account is not active");
        }

        if (user.getPasswordHash() == null) {
            loginAttemptService.loginFailed(attemptKey);
            throw new SecurityException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.loginFailed(attemptKey);
            throw new SecurityException("Invalid email or password");
        }

        loginAttemptService.loginSucceeded(attemptKey);
        // In Phase 3, this will issue JWT tokens
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // Typically user ID comes from JWT context in Phase 3.
        // For Phase 2, this endpoint requires manual user ID injection or testing via token (which is not present).
        throw new UnsupportedOperationException("Not supported until Phase 3 JWT implementation");
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        UUID tenantId = TenantContextHolder.getTenantId();
        Optional<User> userOpt = userRepository.findByEmailAndOrganizationId(request.getEmail(), tenantId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set(RESET_TOKEN_PREFIX + token, user.getId().toString(), RESET_TOKEN_DURATION);
            
            // In a real app, send email here with the token.
            // For now, we rely on the token being in Redis. 
            // In tests we can inspect Redis to grab it.
        }
        // Always return success even if user not found to prevent enumeration
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String userIdStr = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + request.getToken());
        
        if (userIdStr == null) {
            throw new SecurityException("Invalid or expired reset token");
        }

        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));

        if (!isPasswordStrong(request.getNewPassword())) {
            throw new IllegalArgumentException("Password does not meet complexity requirements");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        redisTemplate.delete(RESET_TOKEN_PREFIX + request.getToken());
        
        // Also clear lockouts if any
        loginAttemptService.loginSucceeded(user.getOrganizationId() + ":" + user.getEmail());
    }
    
    // Allows setting a password directly for newly created users in admin flow
    @Transactional
    public void setInitialPassword(UUID userId, String password) {
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
         
         if (!isPasswordStrong(password)) {
             throw new IllegalArgumentException("Password does not meet complexity requirements");
         }
         
         user.setPasswordHash(passwordEncoder.encode(password));
         userRepository.save(user);
    }

    private boolean isPasswordStrong(String password) {
        // Minimal strength check: at least 8 characters
        return password != null && password.length() >= 8;
    }
}
