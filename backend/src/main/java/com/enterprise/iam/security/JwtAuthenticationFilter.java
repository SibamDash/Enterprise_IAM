package com.enterprise.iam.security;

import com.enterprise.iam.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.getClaimsFromToken(jwt);
                
                Boolean isMfa = claims.get("mfa", Boolean.class);
                if (isMfa != null && isMfa) {
                    // This is an intermediate MFA token, not a full access token.
                    // It cannot be used for regular API access.
                    filterChain.doFilter(request, response);
                    return;
                }
                
                String userId = claims.getSubject();
                String tenantIdStr = claims.get("tenantId", String.class);
                String email = claims.get("email", String.class);
                
                if (tenantIdStr != null) {
                    // Make sure TenantContextHolder is populated for the request
                    TenantContextHolder.setTenantId(UUID.fromString(tenantIdStr));
                }

                // Fetch user from DB with roles and groups eagerly
                userRepository.findById(UUID.fromString(userId)).ifPresent(user -> {
                    Set<String> permissions = new HashSet<>();
                    
                    // Add permissions from direct roles
                    if (user.getRoles() != null) {
                        user.getRoles().forEach(role -> {
                            if (role.getPermissions() != null) {
                                permissions.addAll(role.getPermissions());
                            }
                        });
                    }
                    
                    // Add permissions from group roles
                    if (user.getGroups() != null) {
                        user.getGroups().forEach(group -> {
                            if (group.getRoles() != null) {
                                group.getRoles().forEach(role -> {
                                    if (role.getPermissions() != null) {
                                        permissions.addAll(role.getPermissions());
                                    }
                                });
                            }
                        });
                    }

                    var authorities = permissions.stream()
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        String accessTokenParam = request.getParameter("access_token");
        if (StringUtils.hasText(accessTokenParam) && request.getRequestURI().startsWith("/oauth2/")) {
            return accessTokenParam;
        }
        return null;
    }
}
