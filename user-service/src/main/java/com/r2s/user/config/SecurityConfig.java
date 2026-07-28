package com.r2s.user.config;

import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.ApiSecurityHeaders;
import com.r2s.core.security.audit.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final SecurityAuditLogger securityAuditLogger;
    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // cho USER + ADMIN dùng /me
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").hasAnyRole("USER", "ADMIN")

                        // các endpoint còn lại của /users/** bắt buộc ADMIN
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // còn lại: chỉ cần authenticated
                        .anyRequest().authenticated()
                )

                // 🔻 PHẦN QUAN TRỌNG: map 401 & 403
                .exceptionHandling(ex -> ex
                        // Chưa đăng nhập / thiếu token / không có Authentication -> 401
                        .authenticationEntryPoint((request, response, authException) -> {
                            securityAuditLogger.authenticationRejected(
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    request.getRemoteAddr()
                            );
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        // Đã auth nhưng không đủ quyền (sai role) -> 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            securityAuditLogger.authorizationRejected(
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    request.getRemoteAddr(),
                                    request.getUserPrincipal() == null
                                            ? "unknown"
                                            : request.getUserPrincipal().getName()
                            );
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                        })
                )

                .headers(ApiSecurityHeaders.hardenedDefaults())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
