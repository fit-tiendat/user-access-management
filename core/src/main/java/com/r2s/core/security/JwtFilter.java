package com.r2s.core.security;

import com.r2s.core.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // URL authorization belongs to each service's SecurityConfig. This filter
        // only handles requests that actually present a Bearer token.
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            establishSecurityContext(request, token);
        } catch (RuntimeException failure) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void establishSecurityContext(HttpServletRequest request, String token) {
        requireValidSignatureAndExpiry(token);
        String username = requireClaim(jwtService.extractUsername(token));
        String roleClaim = requireClaim(jwtService.extractRole(token));
        List<GrantedAuthority> authorities = authoritiesFrom(roleClaim);
        var authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void requireValidSignatureAndExpiry(String token) {
        if (!jwtService.isSignatureAndExpiryValid(token)) {
            throw new IllegalArgumentException("JWT signature or expiry is invalid");
        }
    }

    private String requireClaim(String claim) {
        if (claim == null || claim.isBlank()) {
            throw new IllegalArgumentException("Required JWT claim is missing");
        }
        return claim;
    }

    private List<GrantedAuthority> authoritiesFrom(String roleClaim) {
        Role role = Role.valueOf(roleClaim);
        return List.of(new SimpleGrantedAuthority(role.name()));
    }
}
