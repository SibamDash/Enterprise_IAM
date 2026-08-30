package com.enterprise.iam.controller;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.dto.MfaDisableRequest;
import com.enterprise.iam.dto.MfaEnableRequest;
import com.enterprise.iam.dto.MfaEnableResponse;
import com.enterprise.iam.dto.MfaSetupResponse;
import com.enterprise.iam.repository.UserRepository;
import com.enterprise.iam.service.MfaService;
import dev.samstevens.totp.exceptions.QrGenerationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;
    private final UserRepository userRepository;

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setup() {
        User user = getAuthenticatedUser();
        if (user.isMfaEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String secret = mfaService.generateSecret();
        try {
            String qrCodeUri = mfaService.getQrCodeDataUri(secret, user.getEmail());
            return ResponseEntity.ok(MfaSetupResponse.builder()
                    .secret(secret)
                    .qrCodeUri(qrCodeUri)
                    .build());
        } catch (QrGenerationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/enable")
    public ResponseEntity<MfaEnableResponse> enable(@Valid @RequestBody MfaEnableRequest request) {
        User user = getAuthenticatedUser();
        if (user.isMfaEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (!mfaService.verifyCode(request.getSecret(), request.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        user.setMfaEnabled(true);
        user.setMfaSecret(request.getSecret());
        userRepository.save(user);

        List<String> recoveryCodes = mfaService.generateRecoveryCodes(user.getId());

        return ResponseEntity.ok(MfaEnableResponse.builder()
                .recoveryCodes(recoveryCodes)
                .build());
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@Valid @RequestBody MfaDisableRequest request) {
        User user = getAuthenticatedUser();
        if (!user.isMfaEnabled()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new SecurityException("Not authenticated");
        }
        UUID userId = UUID.fromString(auth.getPrincipal().toString());
        return userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("User not found"));
    }
}
