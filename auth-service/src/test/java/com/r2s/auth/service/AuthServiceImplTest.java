package com.r2s.auth.service;

import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.security.JwtClaimsBuilder;
import com.r2s.auth.strategy.AuthenticationStrategy;
import com.r2s.auth.strategy.PasswordAuthenticationStrategy;
import com.r2s.core.dto.AuthResponse;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ConflictException;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.JwtService;
import com.r2s.core.security.audit.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // ===== Common mocks (repo / encoder...) =====
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    // ===== Auth mocks =====
    @Mock AuthenticationManager authManager;
    @Mock JwtService jwtService;
    @Mock JwtClaimsBuilder claimsBuilder;
    @Mock SecurityAuditLogger securityAuditLogger;

    // ===== Services under test =====
    RegistrationServiceImpl registrationService;

    // Router service (AuthenticationServiceImpl) depends on List<AuthenticationStrategy>
    AuthenticationServiceImpl authenticationService;

    // Strategy under test
    PasswordAuthenticationStrategy passwordStrategy;

    // Strategy mock to test router behavior
    @Mock AuthenticationStrategy strategyMock;

    @BeforeEach
    void setUp() {
        // Registration service
        registrationService = new RegistrationServiceImpl(
                userRepository,
                passwordEncoder,
                securityAuditLogger
        );

        // Real password strategy (logic login)
        passwordStrategy = new PasswordAuthenticationStrategy(
                authManager,
                userRepository,
                jwtService,
                claimsBuilder,
                securityAuditLogger
        );

        // Authentication router: by default we inject 1 strategy mock
        authenticationService = new AuthenticationServiceImpl(List.of(strategyMock));
    }

    // =========================================================
    // =============== RegistrationServiceImpl tests ============
    // =========================================================

    @Test
    void register_shouldSaveUserWithDefaultRole() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setPassword("1234");
        req.setRole(null); // default ROLE_USER

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        registrationService.register(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        User u = cap.getValue();

        assertThat(u.getUsername()).isEqualTo("john");
        assertThat(u.getPassword()).isEqualTo("ENC");
        assertThat(u.getRole()).isEqualTo(Role.ROLE_USER);
        verify(securityAuditLogger).registrationSucceeded("john", Role.ROLE_USER);
    }

    @Test
    void register_shouldThrowIfDuplicate() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(ConflictException.class, () -> registrationService.register(req));
        verify(userRepository, never()).save(any());
        verify(securityAuditLogger).registrationRejected("john", "duplicate_username");
    }

    // =========================================================
    // =============== AuthenticationServiceImpl (router) ========
    // =========================================================

    @Test
    void authRouter_shouldPickPasswordStrategy_andReturnToken() {
        LoginRequest req = new LoginRequest("john", "1234");

        // router uses authType = "password"
        when(strategyMock.supports("password")).thenReturn(true);
        when(strategyMock.authenticate(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("JWT-TOKEN"));

        AuthResponse res = authenticationService.login(req);

        assertThat(res.getToken()).isEqualTo("JWT-TOKEN");
        verify(strategyMock).supports("password");
        verify(strategyMock).authenticate(req);
    }

    @Test
    void authRouter_shouldThrow_whenNoStrategyFound() {
        LoginRequest req = new LoginRequest("john", "1234");

        when(strategyMock.supports("password")).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> authenticationService.login(req));
        verify(strategyMock).supports("password");
        verify(strategyMock, never()).authenticate(any());
    }

    // =========================================================
    // =============== PasswordAuthenticationStrategy tests ======
    // =========================================================

    @Test
    void passwordStrategy_login_shouldReturnTokenWithRoleClaim() {
        LoginRequest req = new LoginRequest("john", "1234");

        Authentication okAuth = new UsernamePasswordAuthenticationToken("john", null);

        User user = User.builder()
                .username("john")
                .role(Role.ROLE_ADMIN)
                .build();

        when(authManager.authenticate(any())).thenReturn(okAuth);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        when(claimsBuilder.buildClaims(user)).thenReturn(Map.of("role", "ROLE_ADMIN"));
        when(jwtService.generateToken(eq("john"), anyMap())).thenReturn("JWT-TOKEN");

        AuthResponse res = passwordStrategy.authenticate(req);

        assertThat(res.getToken()).isEqualTo("JWT-TOKEN");
        verify(jwtService).generateToken(eq("john"), argThat(m -> "ROLE_ADMIN".equals(m.get("role"))));
        verify(securityAuditLogger).loginSucceeded("john", Role.ROLE_ADMIN);
    }

    @Test
    void passwordStrategy_login_shouldThrowOnBadCredentials() {
        LoginRequest req = new LoginRequest("john", "bad");

        when(authManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> passwordStrategy.authenticate(req));

        verify(userRepository, never()).findByUsername(anyString());
        verify(jwtService, never()).generateToken(anyString(), anyMap());
        verify(securityAuditLogger).loginFailed("john");
    }

    @Test
    void passwordStrategy_login_shouldThrowBadCredentials_whenUserNotFoundAfterSuccessfulAuth() {
        LoginRequest req = new LoginRequest("ghost", "any");

        Authentication okAuth = new UsernamePasswordAuthenticationToken("ghost", null);
        when(authManager.authenticate(any())).thenReturn(okAuth);

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> passwordStrategy.authenticate(req));

        verify(jwtService, never()).generateToken(anyString(), anyMap());
    }
}
