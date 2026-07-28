package com.r2s.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.service.AuthenticationService;
import com.r2s.auth.service.RegistrationService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc // bật full filter chain
@ActiveProfiles("test")
@Import(JwtFilter.class)
@Testcontainers
class AuthControllerJwtFilterIT {

    private static final String BASE = "/api/v1/auth";
    private static final String DUMMY_TOKEN = "any.jwt.token";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    JwtService jwtService;
    @MockBean
    UserDetailsService userDetailsService;

    @MockBean
    RegistrationService registrationService;
    @MockBean
    AuthenticationService authenticationService;

    // ====== Testcontainers Postgres cho profile test ======
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine")
            .withDatabaseName("user_access_management")
            .withUsername("postgres")
            .withPassword("d433221dat");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // ===== helper cho JWT =====
    private String bearerFor(String username, String role) {
        given(jwtService.isSignatureAndExpiryValid(DUMMY_TOKEN)).willReturn(true);
        given(jwtService.extractUsername(DUMMY_TOKEN)).willReturn(username);
        given(jwtService.extractRole(DUMMY_TOKEN)).willReturn(role);
        return "Bearer " + DUMMY_TOKEN;
    }

    // ===== các test cũ giữ nguyên =====

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

        verify(registrationService).register(any(RegisterRequest.class));
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

        verifyNoInteractions(registrationService);
    }

    @Test
    @DisplayName("POST /auth/login: credentials đúng → 200 + token")
    void login_should_return_token_when_valid_credentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice1");
        req.setPassword("secret123");

        AuthResponse resp = new AuthResponse("jwt.token.here");
        given(authenticationService.login(any(LoginRequest.class))).willReturn(resp);

        mockMvc.perform(post(BASE + "/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt.token.here"));

        verify(authenticationService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login: sai password → 401 + message \"Invalid username or password\"")
    void login_should401_when_bad_credentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice1");
        req.setPassword("wrong-pass");

        given(authenticationService.login(any(LoginRequest.class)))
                .willThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post(BASE + "/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    // ===== test BỔ SUNG cho admin-only / quyền =====

    @Test
    @DisplayName("GET /auth/admin-only: thiếu Authorization header → 401")
    void adminOnly_should401_whenMissingAuthorization() throws Exception {
        mockMvc.perform(get(BASE + "/admin-only"))
                .andExpect(status().isUnauthorized());

        // endpoint này không dùng AuthService, đảm bảo không bị đụng
        verifyNoInteractions(authenticationService);
    }

    @Test
    @DisplayName("GET /auth/admin-only: role USER → 403 FORBIDDEN")
    void adminOnly_should403_forUserRole() throws Exception {
        String authHeader = bearerFor("alice", "ROLE_USER");

        mockMvc.perform(get(BASE + "/admin-only")
                .header("Authorization", authHeader))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /auth/admin-only: role ADMIN → 200 OK + body \"ADMIN area\"")
    void adminOnly_should200_forAdminRole() throws Exception {
        String authHeader = bearerFor("admin", "ROLE_ADMIN");

        mockMvc.perform(get(BASE + "/admin-only")
                .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMIN area"));
    }

    @Test
    @DisplayName("GET /auth/admin-only: token invalid → 401")
    void adminOnly_should401_whenTokenInvalid() throws Exception {
        String badToken = "bad.jwt.token";
        given(jwtService.isSignatureAndExpiryValid(badToken)).willReturn(false);

        mockMvc.perform(get(BASE + "/admin-only")
                .header("Authorization", "Bearer " + badToken))
                .andExpect(status().isUnauthorized());
    }
}
