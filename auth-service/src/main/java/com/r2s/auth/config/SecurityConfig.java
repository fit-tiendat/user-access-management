package com.r2s.auth.config;

import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.ApiSecurityHeaders;
import com.r2s.core.security.audit.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // Spring Security 6 / Boot 3
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final SecurityAuditLogger securityAuditLogger;
    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    // cùng property với AuthController: "${api.base-path:/api/v1}"
    @Value("${api.base-path:/api/v1}")
    private String apiBasePath;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // tránh hard-code, nếu sau đổi base-path thì security vẫn đúng
        String registerPath = apiBasePath + "/auth/register";
        String loginPath    = apiBasePath + "/auth/login";

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public health/info
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // chỉ mở đúng 2 endpoint auth public
                        .requestMatchers(registerPath, loginPath).permitAll()
                        // swagger public (nếu dùng)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // còn lại phải có JWT
                        .anyRequest().authenticated()
                )
                // KHÔNG dùng anonymous nữa, để thiếu token thì coi như chưa auth
                .anonymous(anon -> anon.disable())
                // cấu hình 401 / 403 rõ ràng
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            securityAuditLogger.authenticationRejected(
                                    req.getMethod(),
                                    req.getRequestURI(),
                                    req.getRemoteAddr()
                            );
                            res.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            securityAuditLogger.authorizationRejected(
                                    req.getMethod(),
                                    req.getRequestURI(),
                                    req.getRemoteAddr(),
                                    req.getUserPrincipal() == null
                                            ? "unknown"
                                            : req.getUserPrincipal().getName()
                            );
                            res.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden");
                        })
                )
                .headers(ApiSecurityHeaders.hardenedDefaults())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}
