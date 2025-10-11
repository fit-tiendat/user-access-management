package com.r2s.auth.service;


import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.entity.User;


public interface AuthService {
    User register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}