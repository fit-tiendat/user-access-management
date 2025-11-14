package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.security.JwtFilter;
import com.r2s.user.entity.Profile;
import com.r2s.user.exception.ApiExceptionHandler;
import com.r2s.user.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import static org.mockito.Mockito.verify;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.r2s.core.exception.NotFoundException;

@WebMvcTest(controllers = ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)       // tắt security filters trong slice test
@Import(ApiExceptionHandler.class)              // map 404 từ NotFoundException
@ActiveProfiles("test")
class ProfileControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper om;

    @MockBean ProfileService profileService;      // mock business layer
    @MockBean JwtFilter jwtFilter;                // tránh Spring tạo thật JwtFilter (thiếu JwtService)

    @Test
    void list_shouldReturn200_andArray() throws Exception {
        List<Profile> list = List.of(
                Profile.builder().username("alice").fullName("Alice").email("a@mail.com").build(),
                Profile.builder().username("bob").fullName("Bob").email("b@mail.com").build()
        );
        given(profileService.getAll()).willReturn(list);

        mockMvc.perform(get("/api/v1/users").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    void delete_should404_whenServiceThrowsNotFound() throws Exception {
        doThrow(new NotFoundException("profile not found"))
                .when(profileService).deleteByUsername("ghost");

        mockMvc.perform(delete("/api/v1/users/ghost"))
                .andExpect(status().isNotFound());
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
        // doNothing là mặc định của Mockito cho void -> không cần stub

        mockMvc.perform(delete("/api/v1/users/alice"))
                .andExpect(status().isNoContent());

        verify(profileService).deleteByUsername("alice");
    }

}
