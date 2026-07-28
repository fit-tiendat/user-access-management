package com.r2s.core.security.cors;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsPolicyConfigurationTest {

    @Test
    void shouldAllowOnlyConfiguredOriginsWithoutCredentials() {
        CorsPolicyProperties properties = new CorsPolicyProperties();
        properties.setAllowedOrigins(List.of(" https://app.example.com "));

        CorsConfiguration configuration = new CorsPolicyConfiguration(properties)
                .corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("https://app.example.com"))
                .isEqualTo("https://app.example.com");
        assertThat(configuration.checkOrigin("https://evil.example.com")).isNull();
        assertThat(configuration.getAllowCredentials()).isFalse();
    }

    @Test
    void shouldRejectWildcardOriginConfiguration() {
        CorsPolicyProperties properties = new CorsPolicyProperties();
        properties.setAllowedOrigins(List.of("*"));

        assertThatThrownBy(() ->
                new CorsPolicyConfiguration(properties).corsConfigurationSource()
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Wildcard CORS origins are not allowed");
    }
}
