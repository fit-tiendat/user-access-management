package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
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

    @InjectMocks AuthServiceImpl service;

    @Test
    void register_shouldSaveUserWithDefaultRole() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setPassword("1234");
        req.setRole(null); // default ROLE_USER

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("ENC");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        service.register(req);

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
        assertThrows(IllegalArgumentException.class, () -> service.register(req));
    }

    @Test
    void login_shouldReturnTokenWithRoleClaim() {
        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("1234");

        Authentication okAuth = new UsernamePasswordAuthenticationToken("john", null);
        when(authManager.authenticate(any())).thenReturn(okAuth);
        when(userRepository.findByUsername("john"))
                .thenReturn(Optional.of(User.builder().username("john").role(Role.ROLE_ADMIN).build()));
        when(jwtService.generateToken(eq("john"), any(Map.class))).thenReturn("JWT-TOKEN");

        AuthResponse res = service.login(req);

        assertThat(res.getToken()).isEqualTo("JWT-TOKEN");
        verify(jwtService).generateToken(eq("john"), argThat(m -> "ROLE_ADMIN".equals(m.get("role"))));
    }

    @Test
    void login_shouldThrowOnBadCredentials() {
        LoginRequest req = new LoginRequest();
        req.setUsername("john");
        req.setPassword("bad");
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class, () -> service.login(req));
    }
    @Test
    void login_shouldThrowWhenUsernameNotFound() {
        LoginRequest req = new LoginRequest();
        req.setUsername("unknown");
        req.setPassword("any");

        when(authManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> service.login(req));
    }

}
