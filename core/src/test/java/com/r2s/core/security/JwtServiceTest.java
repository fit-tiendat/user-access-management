package com.r2s.core.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    // dùng luôn secret giống trong .env để khỏi nghĩ
    private static final String SECRET =
            "w5h2Yk1Gd3dtdkFzT0h0bTFwM05XUnJvV0V1NHVZbU5yZFRyWW9oUUNmQ1hLcw==";

    private JwtService newService(long expirationMinutes) {
        return new JwtService(SECRET, expirationMinutes, new ObjectMapper());
    }

    @Test
    void generateToken_then_extractUsernameAndRole_shouldMatch() {
        JwtService jwtService = newService(60);

        String username = "alice";
        String role = "ROLE_ADMIN";

        String token = jwtService.generateToken(username, Map.of("role", role));

        String extractedUsername = jwtService.extractUsername(token);
        String extractedRole = jwtService.extractRole(token);

        assertThat(extractedUsername).isEqualTo(username);
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    void isValid_shouldReturnTrue_forCorrectUsername_andNotExpired() {
        JwtService jwtService = newService(60);

        String username = "alice";
        String token = jwtService.generateToken(username, Map.of());

        boolean valid = jwtService.isValid(token, username);

        assertThat(valid).isTrue();
        assertThat(jwtService.isSignatureAndExpiryValid(token)).isTrue();
    }

    @Test
    void isValid_shouldReturnFalse_forWrongUsername_orExpired() {
        JwtService jwtService = newService(60);
        String token = jwtService.generateToken("alice", Map.of());

        // sai username
        boolean validWrongUser = jwtService.isValid(token, "bob");
        assertThat(validWrongUser).isFalse();

        // token hết hạn ngay khi tạo (expirationMinutes = -1)
        JwtService expiredService = newService(-1);
        String expiredToken = expiredService.generateToken("alice", Map.of());

        boolean signatureOk = expiredService.isSignatureAndExpiryValid(expiredToken);
        assertThat(signatureOk).isFalse();  // parse() phải ném ExpiredJwtException
    }
}
