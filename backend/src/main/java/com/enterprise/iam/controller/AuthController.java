package com.enterprise.iam.controller;

import com.enterprise.iam.dto.LoginResponse;
import com.enterprise.iam.dto.ForgotPasswordRequest;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.dto.MfaVerifyRequest;
import com.enterprise.iam.dto.ResetPasswordRequest;
import com.enterprise.iam.service.AuthService;
import com.enterprise.iam.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider tokenProvider;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = httpRequest.getRemoteAddr();
            LoginResponse response = authService.authenticate(request, userAgent, ipAddress);
            establishSession(httpRequest, httpResponse, response);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/login/mfa")
    public ResponseEntity<LoginResponse> loginMfa(@Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = httpRequest.getRemoteAddr();
            LoginResponse response = authService.verifyMfa(request.getMfaToken(), request.getCode(), userAgent, ipAddress);
            establishSession(httpRequest, httpResponse, response);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/login/recovery")
    public ResponseEntity<LoginResponse> loginRecovery(@Valid @RequestBody MfaVerifyRequest request, HttpServletRequest httpRequest) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = httpRequest.getRemoteAddr();
            LoginResponse response = authService.recoverMfa(request.getMfaToken(), request.getCode(), userAgent, ipAddress);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody com.enterprise.iam.dto.RefreshRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = httpRequest.getRemoteAddr();
            LoginResponse response = authService.refreshToken(request, userAgent, ipAddress);
            establishSession(httpRequest, httpResponse, response);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody com.enterprise.iam.dto.RefreshRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            authService.logout(request.getRefreshToken());
            clearSession(httpRequest);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            clearSession(httpRequest);
            return ResponseEntity.ok().build(); // Always return ok to prevent enumeration
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok().build();
        } catch (SecurityException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private void establishSession(HttpServletRequest request, HttpServletResponse response, LoginResponse loginResponse) {
        if (loginResponse.getAccessToken() == null) {
            return; // E.g., MFA required intermediate response
        }
        
        Claims claims = tokenProvider.getClaimsFromToken(loginResponse.getAccessToken());
        String userId = claims.getSubject();
        
        @SuppressWarnings("unchecked")
        java.util.List<String> permissions = claims.get("permissions", java.util.List.class);
        
        java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = java.util.Collections.emptyList();
        if (permissions != null) {
            authorities = permissions.stream()
                    .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                    .collect(java.util.stream.Collectors.toList());
        }
            
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            authorities
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private void clearSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
