package com.enterprise.iam.security;


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
    private final org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.jwt.JwtDecoder> jwtDecoderProvider;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                boolean authenticated = false;
                
                // 1. Try to parse as our local HMAC token
                if (tokenProvider.validateToken(jwt)) {
                    Claims claims = tokenProvider.getClaimsFromToken(jwt);
                    
                    Boolean isMfa = claims.get("mfa", Boolean.class);
                    if (isMfa != null && isMfa) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    String userId = claims.getSubject();
                    String tenantIdStr = claims.get("tenantId", String.class);
                    
                    if (tenantIdStr != null) {
                        TenantContextHolder.setTenantId(UUID.fromString(tenantIdStr));
                    }

                    @SuppressWarnings("unchecked")
                    java.util.List<String> permissions = claims.get("permissions", java.util.List.class);
                    
                    java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = java.util.Collections.emptyList();
                    if (permissions != null) {
                        authorities = permissions.stream()
                                .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                .collect(java.util.stream.Collectors.toList());
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    authenticated = true;
                }
                
                // 2. Try to parse as OAuth2 RSA token if HMAC failed
                if (!authenticated) {
                    try {
                        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
                        if (jwtDecoder != null) {
                            org.springframework.security.oauth2.jwt.Jwt decodedJwt = jwtDecoder.decode(jwt);
                            
                            String userId = decodedJwt.getSubject();
                            String tenantIdStr = decodedJwt.getClaimAsString("tenantId");
                            
                            if (tenantIdStr != null) {
                                TenantContextHolder.setTenantId(UUID.fromString(tenantIdStr));
                            }
                            
                            java.util.List<String> permissions = decodedJwt.getClaimAsStringList("permissions");
                            java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities = java.util.Collections.emptyList();
                            
                            if (permissions != null) {
                                authorities = permissions.stream()
                                        .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                                        .collect(java.util.stream.Collectors.toList());
                            }
                            
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userId, null, authorities);
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        }
                    } catch (org.springframework.security.oauth2.jwt.JwtException e) {
                        // It's neither a valid HMAC nor a valid RSA token
                        logger.warn("Invalid JWT token: " + e.getMessage());
                    }
                }
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
        return null;
    }
}
