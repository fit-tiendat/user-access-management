package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtService;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration-style tests: Security filter chain is ACTIVE.
 * JwtFilter (from core module) is imported, JwtService & UDS are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc // filters ENABLED
@ActiveProfiles("test")
@Import(JwtFilter.class)
class ProfileControllerJwtFilterIT {

    private static final String BASE = "/api/v1/users";
    private static final String DUMMY_TOKEN = "any.jwt.token";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean JwtService jwtService;                 // filter dùng
    @MockBean UserDetailsService userDetailsService; // chỉ để tồn tại bean UDS
    @MockBean ProfileService profileService;       // mock business service

//    @BeforeEach
//    void commonJwtStubs() {
//        // JwtFilter gọi 3 hàm này theo thứ tự: extractUsername -> extractRole -> isSignatureAndExpiryValid
//        // Các test sẽ override stub cụ thể bằng given(...).willReturn(...)
//        given(jwtService.isSignatureAndExpiryValid(anyString())).willReturn(true);
//    }
    @BeforeEach
    void setUp() {
        // Không cần, nhưng để an toàn nếu code sau đổi nhánh:
        given(jwtService.isSignatureAndExpiryValid(anyString())).willReturn(true);
    }
    // helper: cấu hình jwt cho username + role và trả header Authorization
    private String bearerFor(String username, String role) {
        // Phải stub CHÍNH XÁC theo token đang gửi:
        given(jwtService.extractUsername(eq(DUMMY_TOKEN))).willReturn(username);
        given(jwtService.extractRole(eq(DUMMY_TOKEN))).willReturn(role);
        given(jwtService.isValid(eq(DUMMY_TOKEN), eq(username))).willReturn(true);
        return "Bearer " + DUMMY_TOKEN;
    }


    @Test
    @DisplayName("GET /users/me: trả về profile đúng theo username lấy từ JWT")
    void me_returns_own_profile_from_jwt() throws Exception {
        String authHeader = bearerFor("alice", "ROLE_USER");

        Profile alice = Profile.builder()
                .username("alice").fullName("Alice A").email("alice@mail.com").build();
        given(profileService.getByUsername("alice")).willReturn(alice);

        mockMvc.perform(get(BASE + "/me").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.fullName").value("Alice A"))
                .andExpect(jsonPath("$.email").value("alice@mail.com"));

        verify(profileService).getByUsername("alice");
    }

    @Test
    @DisplayName("PUT /users/me: controller ép username theo JWT, bỏ qua username trong body")
    void upsertMe_enforces_username_from_jwt() throws Exception {
        String authHeader = bearerFor("alice", "ROLE_USER");

        // cố ý gửi username khác để kiểm chứng controller ép lại
        ProfileDto body = new ProfileDto("hacker", "New Name", "new@mail.com");

        Profile saved = Profile.builder()
                .username("alice").fullName("New Name").email("new@mail.com").build();

        given(profileService.upsert(argThat(dto ->
                dto.username().equals("alice")
                        && dto.fullName().equals("New Name")
                        && dto.email().equals("new@mail.com")
        ))).willReturn(saved);

        mockMvc.perform(put(BASE + "/me")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.email").value("new@mail.com"));

        verify(profileService).upsert(any(ProfileDto.class));
    }

    @Test
    @DisplayName("GET /users (ADMIN): cho phép & trả về danh sách profile")
    void admin_can_get_all_profiles() throws Exception {
        String authHeader = bearerFor("admin", "ROLE_ADMIN");

        List<Profile> list = List.of(
                Profile.builder().username("alice").fullName("Alice").email("a@mail.com").build(),
                Profile.builder().username("bob").fullName("Bob").email("b@mail.com").build()
        );
        given(profileService.getAll()).willReturn(list);

        mockMvc.perform(get(BASE).header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[1].username").value("bob"));

        verify(profileService).getAll();
    }

    @Test
    @DisplayName("DELETE /users/{username} (ADMIN): trả về 204 & gọi service xoá")
    void admin_can_delete_profile_by_username() throws Exception {
        String authHeader = bearerFor("admin", "ROLE_ADMIN");

        mockMvc.perform(delete(BASE + "/bob").header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        verify(profileService).deleteByUsername("bob");
    }
    @Test
    void me_should401_whenMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("USER gọi /users (ADMIN) → 403")
    void list_should403_whenRoleUser() throws Exception {
        String authHeader = bearerFor("alice", "ROLE_USER");
        mockMvc.perform(get(BASE).header("Authorization", authHeader))
                .andExpect(status().isForbidden());
    }
    @Test
    @DisplayName("Thiếu/invalid token → 401")
    void me_should401_whenTokenInvalid() throws Exception {
        given(jwtService.isSignatureAndExpiryValid(anyString())).willReturn(false);
        mockMvc.perform(get(BASE + "/me").header("Authorization", "Bearer " + DUMMY_TOKEN))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @DisplayName("401 khi có Bearer token nhưng signature/expiry INVALID")
    void any_should401_whenTokenInvalid() throws Exception {
        // gửi token giả, nhưng mock cho INVALID
        String token = "bad.jwt.token";
        given(jwtService.isSignatureAndExpiryValid(eq(token))).willReturn(false);

        mockMvc.perform(get(BASE)               // chọn /users cho dễ
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }


}
