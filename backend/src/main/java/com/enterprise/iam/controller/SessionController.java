package com.enterprise.iam.controller;

import com.enterprise.iam.domain.Session;
import com.enterprise.iam.repository.SessionRepository;
import com.enterprise.iam.security.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionRepository sessionRepository;

    @GetMapping
    public ResponseEntity<List<Session>> getActiveSessions() {
        UUID tenantId = TenantContextHolder.getTenantId();
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        
        List<Session> sessions = sessionRepository.findByUserIdAndTenantIdAndRevokedFalse(userId, tenantId);
        
        // Don't leak the token hashes back to the client
        sessions.forEach(s -> {
            s.setRefreshTokenHash(null);
            s.setTokenFamily(null);
        });
        
        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/{sessionId}")
    @Transactional
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());

        sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId) && s.getTenantId().equals(tenantId))
                .ifPresent(s -> {
                    s.setRevoked(true);
                    sessionRepository.save(s);
                });

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> revokeOtherSessions(@RequestParam("currentSessionId") UUID currentSessionId) {
        UUID tenantId = TenantContextHolder.getTenantId();
        UUID userId = UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        
        sessionRepository.revokeOtherUserSessions(userId, currentSessionId, tenantId);
        
        return ResponseEntity.noContent().build();
    }
}
