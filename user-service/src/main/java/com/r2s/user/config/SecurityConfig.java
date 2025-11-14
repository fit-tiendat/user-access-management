package com.r2s.user.config;

//import com.r2s.user.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor

public class SecurityConfig {
    private final com.r2s.core.security.JwtFilter jwtFilter;

//    private final JwtFilter jwtFilter;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // public
                        .requestMatchers("/actuator/**", "/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // cho USER + ADMIN dùng /me
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/me").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/me").hasAnyRole("USER", "ADMIN")

                        // các endpoint còn lại của /users/** bắt buộc ADMIN
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        // còn lại: chỉ cần authenticated
                        .anyRequest().authenticated()
                )
                .headers(h -> h.frameOptions(frame -> frame.disable()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}