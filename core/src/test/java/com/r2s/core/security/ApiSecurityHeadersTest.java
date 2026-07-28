package com.r2s.core.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiSecurityHeadersTest {

    @Test
    void policiesDenyBrowserCapabilitiesNotRequiredByTheApi() {
        assertThat(ApiSecurityHeaders.CONTENT_SECURITY_POLICY)
                .contains("default-src 'none'", "frame-ancestors 'none'");
        assertThat(ApiSecurityHeaders.PERMISSIONS_POLICY)
                .contains("camera=()", "geolocation=()", "microphone=()");
        assertThat(ApiSecurityHeaders.HSTS_MAX_AGE_SECONDS).isEqualTo(31_536_000L);
    }
}
