package com.enterprise.iam.service;

import com.enterprise.iam.domain.Session;
import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.ChangePasswordRequest;
import com.enterprise.iam.dto.ForgotPasswordRequest;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.dto.LoginResponse;
import com.enterprise.iam.dto.RefreshRequest;
import com.enterprise.iam.dto.ResetPasswordRequest;
import com.enterprise.iam.repository.SessionRepository;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.security.JwtTokenProvider;
import com.enterprise.iam.security.LoginAttemptService;
import com.enterprise.iam.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String RESET_TOKEN_PREFIX = "reset_token:";
    private static final Duration RESET_TOKEN_DURATION = Duration.ofMinutes(30);

    @Transactional
    public LoginResponse authenticate(LoginRequest request, String userAgent, String ipAddress) {
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
        
        // Issue tokens
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getOrganizationId(), user.getEmail());
        String refreshToken = UUID.randomUUID().toString();
        String refreshTokenHash = hashToken(refreshToken);
        
        // Create session
        Session session = new Session();
        session.setUserId(user.getId());
        session.setTenantId(user.getOrganizationId());
        session.setTokenFamily(UUID.randomUUID());
        session.setRefreshTokenHash(refreshTokenHash);
        session.setUserAgent(userAgent);
        session.setIpAddress(ipAddress);
        session.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        sessionRepository.save(session);
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 mins
                .build();
    }

    @Transactional
    public LoginResponse refreshToken(RefreshRequest request, String userAgent, String ipAddress) {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant ID is required");
        }

        String hash = hashToken(request.getRefreshToken());
        Optional<Session> sessionOpt = sessionRepository.findByRefreshTokenHashAndTenantId(hash, tenantId);

        if (sessionOpt.isEmpty()) {
            throw new SecurityException("Invalid refresh token");
        }

        Session session = sessionOpt.get();

        if (session.isRevoked()) {
            // REUSE DETECTED: A revoked token was used.
            // Invalidate the entire token family.
            sessionRepository.revokeTokenFamily(session.getTokenFamily(), tenantId);
            throw new SecurityException("Invalid refresh token (reuse detected)");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setRevoked(true);
            sessionRepository.save(session);
            throw new SecurityException("Refresh token expired");
        }

        // Revoke the old token (rotation)
        session.setRevoked(true);
        sessionRepository.save(session);

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new SecurityException("User not found"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new SecurityException("User is not active");
        }

        // Issue new tokens
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getOrganizationId(), user.getEmail());
        String newRefreshToken = UUID.randomUUID().toString();
        
        Session newSession = new Session();
        newSession.setUserId(user.getId());
        newSession.setTenantId(user.getOrganizationId());
        newSession.setTokenFamily(session.getTokenFamily());
        newSession.setRefreshTokenHash(hashToken(newRefreshToken));
        newSession.setUserAgent(userAgent);
        newSession.setIpAddress(ipAddress);
        newSession.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
        sessionRepository.save(newSession);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        UUID tenantId = TenantContextHolder.getTenantId();
        String hash = hashToken(refreshToken);
        sessionRepository.findByRefreshTokenHashAndTenantId(hash, tenantId)
                .ifPresent(session -> {
                    session.setRevoked(true);
                    sessionRepository.save(session);
                });
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
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
        }
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
        
        loginAttemptService.loginSucceeded(user.getOrganizationId() + ":" + user.getEmail());
    }
    
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

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private boolean isPasswordStrong(String password) {
        // Minimal strength check: at least 8 characters
        return password != null && password.length() >= 8;
    }
}
