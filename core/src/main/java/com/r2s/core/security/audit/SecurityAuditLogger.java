package com.r2s.core.security.audit;

import com.r2s.core.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("SECURITY_AUDIT");
    private static final int MAX_FIELD_LENGTH = 128;

    public void loginSucceeded(String username, Role role) {
        AUDIT_LOG.atInfo()
                .addKeyValue("event", "LOGIN_SUCCESS")
                .addKeyValue("username", clean(username))
                .addKeyValue("role", clean(role))
                .log("Authentication succeeded");
    }

    public void loginFailed(String username) {
        AUDIT_LOG.atWarn()
                .addKeyValue("event", "LOGIN_FAILURE")
                .addKeyValue("username", clean(username))
                .log("Authentication rejected");
    }

    public void registrationSucceeded(String username, Role role) {
        AUDIT_LOG.atInfo()
                .addKeyValue("event", "REGISTRATION_SUCCESS")
                .addKeyValue("username", clean(username))
                .addKeyValue("role", clean(role))
                .log("User registration succeeded");
    }

    public void registrationRejected(String username, String reason) {
        AUDIT_LOG.atWarn()
                .addKeyValue("event", "REGISTRATION_REJECTED")
                .addKeyValue("username", clean(username))
                .addKeyValue("reason", clean(reason))
                .log("User registration rejected");
    }

    public void authenticationRejected(String method, String path, String clientIp) {
        AUDIT_LOG.atWarn()
                .addKeyValue("event", "AUTHENTICATION_REJECTED")
                .addKeyValue("method", clean(method))
                .addKeyValue("path", clean(path))
                .addKeyValue("clientIp", clean(clientIp))
                .log("Unauthenticated request rejected");
    }

    public void authorizationRejected(
            String method,
            String path,
            String clientIp,
            String principal
    ) {
        AUDIT_LOG.atWarn()
                .addKeyValue("event", "AUTHORIZATION_REJECTED")
                .addKeyValue("method", clean(method))
                .addKeyValue("path", clean(path))
                .addKeyValue("clientIp", clean(clientIp))
                .addKeyValue("principal", clean(principal))
                .log("Unauthorized request rejected");
    }

    public void rateLimitExceeded(String endpoint, String clientIp) {
        AUDIT_LOG.atWarn()
                .addKeyValue("event", "RATE_LIMIT_EXCEEDED")
                .addKeyValue("endpoint", clean(endpoint))
                .addKeyValue("clientIp", clean(clientIp))
                .log("Authentication rate limit exceeded");
    }

    private String clean(Object value) {
        if (value == null) {
            return "unknown";
        }

        String cleaned = value.toString()
                .replace('\r', '_')
                .replace('\n', '_')
                .replace('\t', '_');
        return cleaned.length() <= MAX_FIELD_LENGTH
                ? cleaned
                : cleaned.substring(0, MAX_FIELD_LENGTH);
    }
}
