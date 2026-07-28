package com.r2s.core.security;

import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "w5h2Yk1Gd3dtdkFzT0h0bTFwM05XUnJvV0V1NHVZbU5yZFRyWW9oUUNmQ1hLcw==";
    private static final String PREVIOUS_SECRET = Encoders.BASE64.encode(
            "previous-signing-key-with-32-bytes".getBytes(StandardCharsets.UTF_8)
    );

    private JwtService newService(long expirationMinutes) {
        return new JwtService(SECRET, "", expirationMinutes);
    }

    @Test
    void generateToken_thenExtractUsernameAndRole_shouldMatch() {
        JwtService jwtService = newService(60);

        String token = jwtService.generateToken(
                "alice",
                Map.of("role", "ROLE_ADMIN")
        );

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void isValid_shouldReturnTrue_forCorrectUsernameAndValidToken() {
        JwtService jwtService = newService(60);
        String token = jwtService.generateToken("alice", Map.of());

        assertThat(jwtService.isValid(token, "alice")).isTrue();
        assertThat(jwtService.isSignatureAndExpiryValid(token)).isTrue();
    }

    @Test
    void isValid_shouldReturnFalse_forWrongUsername() {
        JwtService jwtService = newService(60);
        String token = jwtService.generateToken("alice", Map.of());

        assertThat(jwtService.isValid(token, "bob")).isFalse();
    }

    @Test
    void previousSigningKey_remainsValidDuringRotation() {
        JwtService previousService = new JwtService(PREVIOUS_SECRET, "", 60);
        String tokenSignedBeforeRotation = previousService.generateToken("alice", Map.of());

        JwtService rotatedService = new JwtService(SECRET, PREVIOUS_SECRET, 60);

        assertThat(rotatedService.isSignatureAndExpiryValid(tokenSignedBeforeRotation)).isTrue();
    }

    @Test
    void constructor_rejectsWeakOrInvalidConfiguration() {
        String weakSecret = Encoders.BASE64.encode(
                "too-short".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> new JwtService(weakSecret, "", 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 256 bits");
        assertThatThrownBy(() -> new JwtService("not-base64!", "", 60))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid Base64");
        assertThatThrownBy(() -> new JwtService(SECRET, "", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }
}
