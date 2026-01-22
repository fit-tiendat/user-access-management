package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;

public interface RegistrationService {
    void register(RegisterRequest request);
}
