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
    void shouldValidatePresentedToken_regardlessOfRequestPath() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/auth/login");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        given(jwtService.isSignatureAndExpiryValid("bad.token")).willReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
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

    @Test
    void shouldReturn401_whenSubjectClaimMissing() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearerToken("missing-subject.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isSignatureAndExpiryValid("missing-subject.token")).willReturn(true);
        given(jwtService.extractUsername("missing-subject.token")).willReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldReturn401_whenRoleClaimMissing() throws ServletException, IOException {
        MockHttpServletRequest request = requestWithBearerToken("missing-role.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtService.isSignatureAndExpiryValid("missing-role.token")).willReturn(true);
        given(jwtService.extractUsername("missing-role.token")).willReturn("alice");
        given(jwtService.extractRole("missing-role.token")).willReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequest requestWithBearerToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/v1/users/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
