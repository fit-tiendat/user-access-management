package com.r2s.auth.strategy;

import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.security.JwtClaimsBuilder;
import com.r2s.core.dto.AuthResponse;
import com.r2s.core.entity.User;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.audit.SecurityAuditLogger;
import com.r2s.core.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PasswordAuthenticationStrategy implements AuthenticationStrategy {

    private final AuthenticationManager authManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final JwtClaimsBuilder claimsBuilder;
    private final SecurityAuditLogger securityAuditLogger;

    @Override
    public boolean supports(String authType) {
        return "password".equalsIgnoreCase(authType);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {
        String username = request.getUsername();

        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

            String token = jwtService.generateToken(
                    user.getUsername(),
                    claimsBuilder.buildClaims(user)
            );

            securityAuditLogger.loginSucceeded(user.getUsername(), user.getRole());

            return new AuthResponse(token);

        } catch (AuthenticationException ex) {
            securityAuditLogger.loginFailed(username);
            throw ex;
        }
    }
}
