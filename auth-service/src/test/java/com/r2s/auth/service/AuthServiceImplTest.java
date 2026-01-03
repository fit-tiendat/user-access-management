package com.r2s.auth.service;

import com.r2s.auth.security.JwtClaimsBuilder;
import com.r2s.core.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ConflictException;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authManager;
    @Mock JwtService jwtService;
    @Mock
    JwtClaimsBuilder claimsBuilder;

    @InjectMocks AuthenticationServiceImpl auth;
    @InjectMocks RegistrationServiceImpl register;

    @Test
    void register_shouldSaveUserWithDefaultRole() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setPassword("1234");
        req.setRole(null); // default ROLE_USER

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        register.register(req);

        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(cap.capture());
        User u = cap.getValue();
        assertThat(u.getUsername()).isEqualTo("john");
        assertThat(u.getPassword()).isEqualTo("ENC");
        assertThat(u.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void register_shouldThrowIfDuplicate() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        when(userRepository.existsByUsername("john")).thenReturn(true);
        assertThrows(ConflictException.class, () -> register.register(req));

    }

    @Test
    void login_shouldReturnTokenWithRoleClaim() {
        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("1234");

        Authentication okAuth =
                new UsernamePasswordAuthenticationToken("john", null);

        User user = User.builder()
                .username("john")
                .role(Role.ROLE_ADMIN)
                .build();

        when(authManager.authenticate(any())).thenReturn(okAuth);
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(user));

        // FIX 2: mock claimsBuilder
        when(claimsBuilder.buildClaims(user))
                .thenReturn(Map.of("role", "ROLE_ADMIN"));

        when(jwtService.generateToken(eq("john"), any(Map.class)))
                .thenReturn("JWT-TOKEN");

        AuthResponse res = auth.login(req);
        assertThat(res.getToken()).isEqualTo("JWT-TOKEN");
        verify(jwtService).generateToken(eq("john"),
                argThat(m -> "ROLE_ADMIN".equals(m.get("role"))));
    }

    @Test
    void login_shouldThrowOnBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("bad");

        when(authManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> auth.login(req));
    }

    @Test
    void login_shouldThrowBadCredentials_whenUserNotFoundAfterSuccessfulAuth() {
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("any");

        // 1) authManager.authenticate thành công
        Authentication okAuth = new UsernamePasswordAuthenticationToken("ghost", null);
        when(authManager.authenticate(any())).thenReturn(okAuth);

        // 2) nhưng repository không tìm thấy user
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        // 3) service phải ném BadCredentialsException (401) như mong đợi
        assertThrows(BadCredentialsException.class, () -> auth.login(req));

        // không được generate JWT với user không tồn tại
        verify(jwtService, never()).generateToken(anyString(), any(Map.class));
    }


}
