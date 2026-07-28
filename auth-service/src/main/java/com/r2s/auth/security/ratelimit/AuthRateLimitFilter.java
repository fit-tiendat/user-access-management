package com.r2s.auth.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.r2s.core.dto.ApiErrorCode;
import com.r2s.core.dto.ApiResponse;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Profile("!test")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_POLICY = "login";
    private static final String REGISTER_POLICY = "register";

    private final AuthRateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Cache<String, Bucket> buckets;
    private final String loginPath;
    private final String registerPath;

    public AuthRateLimitFilter(
            AuthRateLimitProperties properties,
            ObjectMapper objectMapper,
            @Value("${api.base-path:/api/v1}") String apiBasePath
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        String normalizedBasePath = apiBasePath.endsWith("/")
                ? apiBasePath.substring(0, apiBasePath.length() - 1)
                : apiBasePath;
        this.loginPath = normalizedBasePath + "/auth/login";
        this.registerPath = normalizedBasePath + "/auth/register";

        Duration cacheTtl = max(
                properties.getLogin().getRefillPeriod(),
                properties.getRegister().getRefillPeriod()
        ).multipliedBy(2);
        this.buckets = Caffeine.newBuilder()
                .maximumSize(properties.getMaxTrackedClients())
                .expireAfterAccess(cacheTtl)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.isEnabled()
                || !HttpMethod.POST.matches(request.getMethod())
                || resolvePolicy(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String policyName = resolvePolicy(request);
        AuthRateLimitProperties.Limit limit = resolveLimit(policyName);
        String clientIp = request.getRemoteAddr();
        String bucketKey = policyName + ":" + clientIp;

        Bucket bucket = buckets.get(bucketKey, ignored -> newBucket(limit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Limit", Long.toString(limit.getCapacity()));
        response.setHeader("X-RateLimit-Remaining", Long.toString(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(
                1,
                (long) Math.ceil(
                        (double) probe.getNanosToWaitForRefill()
                                / TimeUnit.SECONDS.toNanos(1)
                )
        );

        log.warn("Rate limit exceeded: endpoint={}, clientIp={}", policyName, clientIp);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.failed(
                        ApiErrorCode.RATE_LIMIT_EXCEEDED.name(),
                        "Too many authentication attempts. Please try again later."
                )
        );
    }

    private String resolvePolicy(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals(loginPath)) {
            return LOGIN_POLICY;
        }
        if (path.equals(registerPath)) {
            return REGISTER_POLICY;
        }
        return null;
    }

    private AuthRateLimitProperties.Limit resolveLimit(String policyName) {
        return LOGIN_POLICY.equals(policyName)
                ? properties.getLogin()
                : properties.getRegister();
    }

    private Bucket newBucket(AuthRateLimitProperties.Limit limit) {
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit.getCapacity())
                        .refillIntervally(limit.getCapacity(), limit.getRefillPeriod()))
                .build();
    }

    private Duration max(Duration first, Duration second) {
        return first.compareTo(second) >= 0 ? first : second;
    }
}
