package com.enterprise.iam.controller;

import com.enterprise.iam.dto.LoginResponse;
import com.enterprise.iam.dto.ForgotPasswordRequest;
import com.enterprise.iam.dto.LoginRequest;
import com.enterprise.iam.dto.MfaVerifyRequest;
import com.enterprise.iam.dto.ResetPasswordRequest;
import com.enterprise.iam.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            String userAgent = httpRequest.getHeader("User-Agent");
            String ipAddress = httpRequest.getRemoteAddr();
            LoginResponse response = authService.authenticate(request, userAgent, ipAddress);
            addSessionCookie(httpResponse, response.getAccessToken());
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
            addSessionCookie(httpResponse, response.getAccessToken());
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
            addSessionCookie(httpResponse, response.getAccessToken());
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody com.enterprise.iam.dto.RefreshRequest request, HttpServletResponse httpResponse) {
        try {
            authService.logout(request.getRefreshToken());
            clearSessionCookie(httpResponse);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            clearSessionCookie(httpResponse);
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

    private void addSessionCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("IAM_SESSION", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Should be true in production HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(15 * 60); // Match token validity (15 mins)
        response.addCookie(cookie);
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("IAM_SESSION", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
