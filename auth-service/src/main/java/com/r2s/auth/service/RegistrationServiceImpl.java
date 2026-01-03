package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ConflictException;
import com.r2s.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername();

        if (userRepository.existsByUsername(username)) {
            log.warn("Register rejected: duplicate username={}", username);
            throw new ConflictException("Username already exists");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.ROLE_USER;

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        log.info("Register success: username={}, role={}", username, role);
    }
}
