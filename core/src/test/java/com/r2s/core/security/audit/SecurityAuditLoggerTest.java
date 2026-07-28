package com.r2s.core.security.audit;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.r2s.core.entity.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditLoggerTest {

    private final SecurityAuditLogger securityAuditLogger = new SecurityAuditLogger();
    private final Logger auditLogger = (Logger) LoggerFactory.getLogger("SECURITY_AUDIT");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        auditLogger.setLevel(Level.ALL);
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        auditLogger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void loginSucceeded_shouldWriteStructuredFieldsWithoutCredentials() {
        securityAuditLogger.loginSucceeded("alice", Role.ROLE_USER);

        ILoggingEvent event = appender.list.get(0);
        Map<String, String> fields = fields(event);

        assertThat(fields)
                .containsEntry("event", "LOGIN_SUCCESS")
                .containsEntry("username", "alice")
                .containsEntry("role", "ROLE_USER")
                .doesNotContainKeys("password", "token", "jwt");
    }

    @Test
    void clientControlledFields_shouldNotCreateMultilineLogEntries() {
        securityAuditLogger.loginFailed("alice\nforged-event");

        Map<String, String> fields = fields(appender.list.get(0));

        assertThat(fields.get("username"))
                .isEqualTo("alice_forged-event")
                .doesNotContain("\n", "\r", "\t");
    }

    private Map<String, String> fields(ILoggingEvent event) {
        return event.getKeyValuePairs().stream()
                .collect(Collectors.toMap(
                        pair -> pair.key,
                        pair -> String.valueOf(pair.value)
                ));
    }
}
