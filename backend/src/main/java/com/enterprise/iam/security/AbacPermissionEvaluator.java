package com.enterprise.iam.security;

import com.enterprise.iam.domain.User;
import com.enterprise.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AbacPermissionEvaluator implements PermissionEvaluator {

    private final PolicyEngine policyEngine;
    private final UserRepository userRepository;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(permission instanceof String)) {
            return false;
        }

        UUID userId = extractUserId(authentication);
        if (userId == null) return false;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        String action = (String) permission;
        String resourceName = getResourceName(targetDomainObject);
        Map<String, Object> resourceAttributes = getResourceAttributes(targetDomainObject);

        return policyEngine.evaluate(user, action, resourceName, resourceAttributes);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !(permission instanceof String)) {
            return false;
        }

        UUID userId = extractUserId(authentication);
        if (userId == null) return false;

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        String action = (String) permission;
        
        // When using hasPermission(id, type, permission), we don't have the full object.
        // The resourceName is the type, and attributes are empty unless we load it.
        // For ABAC foundation, evaluating just by type without attributes.
        return policyEngine.evaluate(user, action, targetType, Collections.emptyMap());
    }

    private UUID extractUserId(Authentication authentication) {
        try {
            return UUID.fromString(authentication.getName()); // Assuming JWT principal is the UUID
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String getResourceName(Object targetDomainObject) {
        if (targetDomainObject == null) return "global";
        if (targetDomainObject instanceof String) return (String) targetDomainObject;
        return targetDomainObject.getClass().getSimpleName();
    }

    private Map<String, Object> getResourceAttributes(Object targetDomainObject) {
        if (targetDomainObject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) targetDomainObject;
            return map;
        }
        // In a full implementation, use reflection or Jackson to convert object properties to Map
        return Collections.emptyMap();
    }
}
