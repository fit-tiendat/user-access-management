// core/src/main/java/com/r2s/core/security/JwtFilter.java
package com.r2s.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectProvider<UserDetailsService> udsProvider; // optional

    public JwtFilter(JwtService jwtService, ObjectProvider<UserDetailsService> udsProvider) {
        this.jwtService = jwtService;
        this.udsProvider = udsProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);
        String role = jwtService.extractRole(token); // "ROLE_USER"...

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetailsService uds = udsProvider.getIfAvailable();

            boolean valid = (uds != null)
                    ? jwtService.isValid(token, username)            // auth-service
                    : jwtService.isSignatureAndExpiryValid(token);   // user-service

            if (valid) {
                // 👉 Gõ kiểu tường minh để khớp với constructor
                List<? extends GrantedAuthority> authorities =
                        (role == null)
                                ? Collections.<SimpleGrantedAuthority>emptyList()
                                : List.of(new SimpleGrantedAuthority(role));

                var authToken = new UsernamePasswordAuthenticationToken(
                        username, null, authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
