package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ConflictException;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.audit.SecurityAuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    SecurityAuditLogger securityAuditLogger;

    @InjectMocks
    RegistrationServiceImpl registrationService;

    @Test
    void register_shouldSaveUserWithDefaultRole() {
        // given
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");
        req.setPassword("1234");
        req.setRole(null); // default ROLE_USER

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("ENCODED");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // when
        registrationService.register(req);

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("john");
        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void register_shouldThrowConflictException_whenUsernameExists() {
        // given
        RegisterRequest req = new RegisterRequest();
        req.setUsername("john");

        when(userRepository.existsByUsername("john")).thenReturn(true);

        // when + then
        assertThrows(ConflictException.class,
                () -> registrationService.register(req));

        verify(userRepository, never()).save(any());
    }
}
