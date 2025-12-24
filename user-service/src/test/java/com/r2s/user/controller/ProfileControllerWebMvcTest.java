package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.exception.GlobalExceptionHandler;
import com.r2s.core.exception.NotFoundException;
import com.r2s.core.security.JwtFilter;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.service.ProfileCommandService;
import com.r2s.user.service.ProfileQueryService;
import com.r2s.core.utils.ResponseBuilder;
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
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class,ResponseBuilder.class})
@ActiveProfiles("test")
class ProfileControllerWebMvcTest {


    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean ProfileCommandService commandService;
    @MockBean ProfileQueryService queryService;

    // addFilters=false nên JwtFilter không chạy, nhưng giữ mock để context khỏi thiếu bean nếu cần
    @MockBean JwtFilter jwtFilter;

    private Principal principal(String username) {
        return () -> username;
    }

    @Test
    void list_shouldReturn200_andArray() throws Exception {
        List<Profile> list = List.of(
                Profile.builder().id(1L).username("alice").fullName("Alice").email("a@mail.com").build(),
                Profile.builder().id(2L).username("bob").fullName("Bob").email("b@mail.com").build()
        );
        given(queryService.getAll()).willReturn(list);

        mockMvc.perform(get("/api/v1/users").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[1].username").value("bob"));
    }

    @Test
    void list_shouldReturn200_andEmptyArray() throws Exception {
        given(queryService.getAll()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/users").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void delete_should204_andInvokeService() throws Exception {
        mockMvc.perform(delete("/api/v1/users/alice"))
                .andExpect(status().isNoContent());

        verify(commandService).deleteByUsername("alice");
    }

    @Test
    void delete_should404_whenServiceThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("profile not found"))
                .when(commandService).deleteByUsername("ghost");

        mockMvc.perform(delete("/api/v1/users/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message").value("profile not found"));
    }

    @Test
    void getMe_shouldReturnProfile_ofCurrentUser() throws Exception {
        Profile p = Profile.builder()
                .id(1L)
                .username("alice")
                .fullName("Alice")
                .email("alice@mail.com")
                .build();
        given(queryService.getByUsername("alice")).willReturn(p);

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(principal("alice"))
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@mail.com"));

        verify(queryService).getByUsername("alice");
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
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.message", containsString("email")))
                .andExpect(jsonPath("$.message", containsString("must be a well-formed email address")));

        verifyNoInteractions(commandService, queryService);
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

        given(commandService.upsert(any(ProfileDto.class))).willReturn(saved);

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(principal("alice"))
                        .contentType(APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.username").value("alice"));

        ArgumentCaptor<ProfileDto> captor = ArgumentCaptor.forClass(ProfileDto.class);
        verify(commandService).upsert(captor.capture());
        ProfileDto dtoUsed = captor.getValue();

        assertThat(dtoUsed.username()).isEqualTo("alice");
    }
}
