package com.r2s.auth.service;

import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.strategy.AuthenticationStrategy;
import com.r2s.core.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final List<AuthenticationStrategy> strategies;

    @Override
    public AuthResponse login(LoginRequest request) {
        String authType = "password"; // vì LoginRequest hiện tại chưa có authType

        return strategies.stream()
                .filter(s -> s.supports(authType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No auth strategy found for type: " + authType))
                .authenticate(request);
    }
}
