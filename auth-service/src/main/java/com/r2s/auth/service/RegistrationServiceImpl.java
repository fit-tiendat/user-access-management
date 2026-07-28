package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ConflictException;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.audit.SecurityAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditLogger securityAuditLogger;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername();

        if (userRepository.existsByUsername(username)) {
            securityAuditLogger.registrationRejected(username, "duplicate_username");
            throw new ConflictException("Username already exists");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
        securityAuditLogger.registrationSucceeded(username, Role.ROLE_USER);
    }
}
