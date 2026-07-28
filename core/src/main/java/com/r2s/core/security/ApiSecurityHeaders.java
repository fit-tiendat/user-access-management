package com.r2s.core.security;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Shared browser security headers for the stateless JSON APIs.
 */
public final class ApiSecurityHeaders {

    static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";
    static final String PERMISSIONS_POLICY =
            "accelerometer=(), camera=(), geolocation=(), microphone=(), payment=(), usb=()";
    static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

    private ApiSecurityHeaders() {
    }

    public static Customizer<HeadersConfigurer<HttpSecurity>> hardenedDefaults() {
        return headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS));
    }
}
