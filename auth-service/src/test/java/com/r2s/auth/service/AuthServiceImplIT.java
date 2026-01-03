package com.r2s.auth.service;

import com.r2s.core.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AuthServiceImplIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("user_access_management")
                    .withUsername("postgres")
                    .withPassword("d433221dat");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    AuthenticationService authService;
    @Autowired
    RegistrationService registrationService;

    @Test
    @DisplayName("register: đăng ký user mới -> login lại được, trả JWT")
    void register_then_login_success() {
        // given
        RegisterRequest registerReq = new RegisterRequest(
                "alice_auth",   // username
                "@P4ssw0rd",    // password
                null            // role (cho null, service tự handle)
        );

        // when: chỉ cần gọi register, không cần giá trị trả về
        registrationService.register(registerReq);

        // then: login lại phải thành công và trả token
        LoginRequest loginReq = new LoginRequest(
                "alice_auth",
                "@P4ssw0rd"
        );

        AuthResponse loginRes = authService.login(loginReq);

        assertThat(loginRes).isNotNull();
        assertThat(loginRes.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("login: sai password -> BadCredentialsException")
    void login_wrong_password() {
        // given: tạo trước 1 user hợp lệ
        RegisterRequest registerReq = new RegisterRequest(
                "bob_auth",
                "@P4ssw0rd",
                null
        );
        registrationService.register(registerReq);

        // when + then: login sai password -> ném BadCredentialsException
        LoginRequest loginReq = new LoginRequest(
                "bob_auth",
                "wrong-password"
        );

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginReq)
        );
    }
}
