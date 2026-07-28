package com.r2s.auth.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AuthRateLimitFilterTest {

    private final AuthRateLimitProperties properties = new AuthRateLimitProperties();
    private final AuthRateLimitFilter filter = new AuthRateLimitFilter(
            properties,
            new ObjectMapper().findAndRegisterModules(),
            "/api/v1"
    );
    private final FilterChain filterChain = mock(FilterChain.class);

    @Test
    void login_shouldReturn429AfterFiveRequestsFromSameIp() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            MockHttpServletResponse response = execute("/api/v1/auth/login", "192.0.2.10");
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        MockHttpServletResponse rejected = execute("/api/v1/auth/login", "192.0.2.10");

        assertThat(rejected.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(rejected.getHeader("Retry-After")).isNotBlank();
        assertThat(rejected.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(rejected.getContentAsString()).contains("Too many authentication attempts");
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    void register_shouldReturn429AfterThreeRequestsFromSameIp() throws Exception {
        for (int attempt = 0; attempt < 3; attempt++) {
            assertThat(execute("/api/v1/auth/register", "192.0.2.20").getStatus())
                    .isEqualTo(HttpStatus.OK.value());
        }

        MockHttpServletResponse rejected = execute("/api/v1/auth/register", "192.0.2.20");

        assertThat(rejected.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    @Test
    void limits_shouldBeIndependentForDifferentClientIps() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            execute("/api/v1/auth/login", "192.0.2.30");
        }

        MockHttpServletResponse otherClient = execute("/api/v1/auth/login", "192.0.2.31");

        assertThat(otherClient.getStatus()).isEqualTo(HttpStatus.OK.value());
        verify(filterChain, times(6)).doFilter(any(), any());
    }

    @Test
    void nonAuthenticationEndpoint_shouldNotBeRateLimited() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            execute("/api/v1/auth/admin-only", "192.0.2.40");
        }

        verify(filterChain, times(10)).doFilter(any(), any());
    }

    private MockHttpServletResponse execute(String path, String remoteAddress) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddress);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);
        return response;
    }
}
