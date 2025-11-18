package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.exception.NotFoundException;
import com.r2s.core.security.JwtFilter;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.dto.ProfileResponse;
import com.r2s.user.entity.Profile;
import com.r2s.user.exception.ApiExceptionHandler;
import com.r2s.user.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)   // tắt filter chain (JWT, v.v.)
@Import(ApiExceptionHandler.class)         // dùng ApiExceptionHandler hiện tại
@ActiveProfiles("test")
class ProfileControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean ProfileService profileService;
    @MockBean JwtFilter jwtFilter;   // để context security không bị thiếu bean

    // helper fake principal
    private Principal principal(String username) {
        return () -> username;
    }

    // ====== ADMIN endpoint /api/v1/users ======

    @Test
    void list_shouldReturn200_andArray() throws Exception {
        List<Profile> list = List.of(
                Profile.builder().id(1L).username("alice").fullName("Alice").email("a@mail.com").build(),
                Profile.builder().id(2L).username("bob").fullName("Bob").email("b@mail.com").build()
        );
        given(profileService.getAll()).willReturn(list);

        mockMvc.perform(get("/api/v1/users").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("[0].username").value("alice"))
                .andExpect(jsonPath("[1].username").value("bob"));
    }

    @Test
    void list_shouldReturn200_andEmptyArray() throws Exception {
        given(profileService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/users").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void delete_should204_andInvokeService() throws Exception {
        mockMvc.perform(delete("/api/v1/users/alice"))
                .andExpect(status().isNoContent());

        verify(profileService).deleteByUsername("alice");
    }

    @Test
    void delete_should404_whenServiceThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("profile not found"))
                .when(profileService).deleteByUsername("ghost");

        mockMvc.perform(delete("/api/v1/users/ghost"))
                .andExpect(status().isNotFound());
    }

    // ====== /api/v1/users/me ======

    @Test
    void getMe_shouldReturnProfile_ofCurrentUser() throws Exception {
        Profile p = Profile.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .email("alice@mail.com")
                .build();
        given(profileService.getByUsername("alice")).willReturn(p);

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(principal("alice"))
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@mail.com"));

        verify(profileService).getByUsername("alice");
    }

    @Test
    void updateMe_should400_whenInvalidEmail() throws Exception {
        String json = """
                {
                  "username": "alice",
                  "fullName": "Alice",
                  "email": "not-an-email"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(principal("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                // body hiện tại là text/plain: "email: must be a well-formed email address"
                .andExpect(content().string(containsString("email")))
                .andExpect(content().string(containsString("must be a well-formed email address")));

        // validation fail nên service không được gọi
        verifyNoInteractions(profileService);
    }

    @Test
    void updateMe_shouldForceUsernameFromToken_evenIfBodyCheated() throws Exception {
        String json = """
                {
                  "username": "hacker",
                  "fullName": "Alice",
                  "email": "alice@mail.com"
                }
                """;

        Profile saved = Profile.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .email("alice@mail.com")
                .build();

        given(profileService.upsert(any(ProfileDto.class))).willReturn(saved);

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(principal("alice"))   // user thực tế là alice
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // bắt argument truyền xuống service
        ArgumentCaptor<ProfileDto> captor = ArgumentCaptor.forClass(ProfileDto.class);
        verify(profileService).upsert(captor.capture());
        ProfileDto dtoUsed = captor.getValue();

        // username phải bị ép lại thành "alice", không phải "hacker"
        assertThat(dtoUsed.username()).isEqualTo("alice");
    }
}
