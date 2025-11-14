package com.r2s.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.service.AuthService;
import com.r2s.core.entity.Role;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc   // bật full filter chain
@ActiveProfiles("test")
@Import(JwtFilter.class)
class AuthControllerJwtFilterIT {

    private static final String BASE = "/api/v1/auth";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtService jwtService;
    @MockBean UserDetailsService userDetailsService;
    @MockBean AuthService authService;

    @Test
    @DisplayName("POST /auth/register: valid body → 200 OK + message, gọi service")
    void register_should200_when_valid() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice1");
        req.setPassword("secret123");
        req.setRole(Role.ROLE_USER);

        mockMvc.perform(post(BASE + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register: username quá ngắn → 400 + field error, không gọi service")
    void register_should400_when_invalid_body() throws Exception {
        String json = """
                {
                  "username": "abc",
                  "password": "123456"
                }
                """;

        mockMvc.perform(post(BASE + "/register")
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").exists());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/login: credentials đúng → 200 + token")
    void login_should_return_token_when_valid_credentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice1");
        req.setPassword("secret123");

        AuthResponse resp = new AuthResponse("jwt.token.here");
        given(authService.login(any(LoginRequest.class))).willReturn(resp);

        mockMvc.perform(post(BASE + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt.token.here"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login: sai password → 401 + message \"Invalid username or password\"")
    void login_should401_when_bad_credentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice1");
        req.setPassword("wrong-pass");

        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post(BASE + "/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid username or password")));
    }
}
