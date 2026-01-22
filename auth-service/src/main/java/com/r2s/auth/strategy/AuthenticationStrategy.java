package com.r2s.auth.strategy;

import com.r2s.auth.dto.LoginRequest;
import com.r2s.core.dto.AuthResponse;

public interface AuthenticationStrategy {
    boolean supports(String authType);
    AuthResponse authenticate(LoginRequest request);
}
