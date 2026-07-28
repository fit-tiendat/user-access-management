package com.r2s.auth.security.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "security.rate-limit")
public class AuthRateLimitProperties {

    private boolean enabled = true;

    @Min(1)
    private long maxTrackedClients = 100_000;

    @Valid
    private Limit login = new Limit(5, Duration.ofMinutes(15));

    @Valid
    private Limit register = new Limit(3, Duration.ofHours(1));

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Limit {

        @Min(1)
        private long capacity;

        @NotNull
        private Duration refillPeriod;
    }
}
