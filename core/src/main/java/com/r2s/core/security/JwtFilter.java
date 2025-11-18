package com.r2s.core.security;

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
import java.util.Collections;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // 1) Bỏ qua hoàn toàn cho các endpoint public: login/register, swagger, actuator...
        boolean isPublicAuthEndpoint =
                path.equals("/api/v1/auth/register") ||
                        path.equals("/api/v1/auth/login")    ||
                        path.equals("/auth/register")        ||
                        path.equals("/auth/login");

        if (isPublicAuthEndpoint
                || path.startsWith("/actuator")
                || path.startsWith("/error")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 2) Lấy header Authorization. Nếu không có / sai format -> KHÔNG tự 401,
        // cứ cho đi tiếp để Spring Security tự xử lý (endpoint yêu cầu auth sẽ 401).
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 3) Check chữ ký + expiry của JWT
            if (!jwtService.isSignatureAndExpiryValid(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
                return;
            }

            // 4) Lấy thông tin từ token
            String username = jwtService.extractUsername(token);
            String roleClaim = jwtService.extractRole(token);

            // Nếu chưa có Authentication trong SecurityContext thì set vào
            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String authorityName = null;
                if (roleClaim != null) {
                    authorityName = roleClaim.startsWith("ROLE_")
                            ? roleClaim
                            : "ROLE_" + roleClaim;
                }

                List<GrantedAuthority> authorities =
                        authorityName == null
                                ? Collections.emptyList()
                                : List.of(new SimpleGrantedAuthority(authorityName));

                var authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

        } catch (RuntimeException ex) {
            // Bất kỳ lỗi parse/validate JWT nào -> 401
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT");
            return;
        }

        // 5) Cho request đi tiếp
        filterChain.doFilter(request, response);
    }
}
