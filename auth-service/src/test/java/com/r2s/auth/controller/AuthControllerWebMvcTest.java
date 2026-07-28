package com.r2s.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.dto.AuthResponse;
import com.r2s.core.exception.ConflictException;
import com.r2s.core.exception.GlobalExceptionHandler;
import com.r2s.auth.service.AuthenticationService;
import com.r2s.auth.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper om;
    @MockBean
    com.r2s.core.security.JwtFilter jwtFilter;
    @MockBean
    AuthenticationService authService;
    @MockBean
    RegistrationService register;

    @Test
    void register_shouldReturn200_andSuccessMessage() throws Exception {
        // build JSON trực tiếp để khỏi cần constructor DTO
        String body = """
                  {"username":"john","password":"Strong@123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void login_shouldReturn200_andToken() throws Exception {
        // mock service trả về AuthResponse bằng constructor có tham số
        when(authService.login(any()))
                .thenReturn(new AuthResponse("jwt-token")); // <-- sửa ở đây

        String body = """
                  {"username":"john","password":"secret123"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }


    @Test
    void register_should400_whenUsernameHasWhitespace() throws Exception {
        String body = """
                  {"username":"john doe","password":"Strong@123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_should400_whenPasswordDoesNotMeetPolicy() throws Exception {
        String body = """
                  {"username":"john","password":"password123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void login_should401_onBadCredentials() throws Exception {
        when(authService.login(any()))
                .thenThrow(new BadCredentialsException("bad creds"));

        String body = """
                {"username":"john","password":"bad"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_should400_whenUsernameHasWhitespace_andReturnErrorMap() throws Exception {
        String body = """
                  {"username":"john doe","password":"Strong@123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.username").exists());
    }

    @Test
    void login_should401_andMessage_onBadCredentials() throws Exception {
        // ném BadCredentialsException từ service
        when(authService.login(any())).thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad"));

        String body = """
                  {"username":"john","password":"bad"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void register_should409_andUseStandardResponse_whenUsernameExists() throws Exception {
        doThrow(new ConflictException("Username already exists"))
                .when(register).register(any());

        String body = """
                  {"username":"john","password":"Strong@123"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    void login_should500_withoutLeakingInternalExceptionMessage() throws Exception {
        when(authService.login(any()))
                .thenThrow(new RuntimeException("database-password=do-not-leak"));

        String body = """
                  {"username":"john","password":"secret123"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(content().string(not(containsString("database-password"))));
    }

}
