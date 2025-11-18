package com.r2s.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    JwtService jwtService;

    @Mock
    FilterChain filterChain;

    @InjectMocks
    JwtFilter jwtFilter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldBypassFilter_forPublicPaths() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login"); // path public
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueWithoutAuth_whenNoAuthorizationHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldSetAuthentication_whenTokenValid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer good.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isSignatureAndExpiryValid("good.token")).willReturn(true);
        given(jwtService.extractUsername("good.token")).willReturn("alice");
        given(jwtService.extractRole("good.token")).willReturn("ROLE_ADMIN");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("alice");
        assertThat(auth.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldReturn401_andNotCallChain_whenTokenInvalid() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isSignatureAndExpiryValid("bad.token")).willReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void shouldReturn401_whenJwtServiceThrowsRuntimeException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer broken.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isSignatureAndExpiryValid("broken.token"))
                .willThrow(new RuntimeException("parse error"));

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
