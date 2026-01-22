package com.r2s.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtService;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.repository.ProfileRepository;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc // filters ENABLED
@ActiveProfiles("test")
@Import(JwtFilter.class)
@Testcontainers
class ProfileControllerJwtFilterIT {

    private static final String BASE = "/api/v1/users";
    private static final String DUMMY_TOKEN = "any.jwt.token";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine")
            .withDatabaseName("user_access_management")
            .withUsername("postgres")
            .withPassword("d433221dat");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ProfileRepository profileRepository;

    @MockBean
    JwtService jwtService;
    @MockBean
    UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        given(jwtService.isSignatureAndExpiryValid(anyString())).willReturn(true);
    }

    private String bearerFor(String username, String role) {
        given(jwtService.extractUsername(eq(DUMMY_TOKEN))).willReturn(username);
        given(jwtService.extractRole(eq(DUMMY_TOKEN))).willReturn(role);
        given(jwtService.isValid(eq(DUMMY_TOKEN), eq(username))).willReturn(true);
        return "Bearer " + DUMMY_TOKEN;
    }

    @Test
    @DisplayName("GET /users/me: trả về profile đúng theo username lấy từ JWT (DB thật)")
    void me_returns_own_profile_from_jwt() throws Exception {
        String authHeader = bearerFor("alice", "ROLE_USER");

        Profile alice = Profile.builder()
                .username("alice")
                .fullName("Alice A")
                .email("alice@mail.com")
                .build();
        profileRepository.save(alice);

        mockMvc.perform(get(BASE + "/me").header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.fullName").value("Alice A"))
                .andExpect(jsonPath("$.data.email").value("alice@mail.com"));

        Profile inDb = profileRepository.findByUsername("alice").orElseThrow();
        assertEquals("Alice A", inDb.getFullName());
        assertEquals("alice@mail.com", inDb.getEmail());
    }

    @Test
    @DisplayName("PUT /users/me: username lấy từ JWT, body không còn username (DB thật)")
    void upsertMe_enforces_username_from_jwt() throws Exception {
        String authHeader = bearerFor("alice", "ROLE_USER");

        // body chỉ còn fullName, email
        ProfileDto body = new ProfileDto("New Name", "new@mail.com");

        mockMvc.perform(put(BASE + "/me")
                .header("Authorization", authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.fullName").value("New Name"))
                .andExpect(jsonPath("$.data.email").value("new@mail.com"));

        Profile inDb = profileRepository.findByUsername("alice").orElseThrow();
        assertEquals("New Name", inDb.getFullName());
        assertEquals("new@mail.com", inDb.getEmail());

        // chắc chắn không có "hacker"
        assertTrue(profileRepository.findByUsername("hacker").isEmpty());
    }

    @Test
    @DisplayName("GET /users (ADMIN): cho phép & trả về danh sách profile từ DB")
    void admin_can_get_all_profiles() throws Exception {
        String authHeader = bearerFor("admin", "ROLE_ADMIN");

        profileRepository.saveAll(List.of(
                Profile.builder().username("alice").fullName("Alice").email("a@mail.com").build(),
                Profile.builder().username("bob").fullName("Bob").email("b@mail.com").build()));

        mockMvc.perform(get(BASE).header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username").value("alice"))
                .andExpect(jsonPath("$.data[1].username").value("bob"));
    }

    @Test
    @DisplayName("DELETE /users/{username} (ADMIN): trả về 204 & thực sự xoá trong DB")
    void admin_can_delete_profile_by_username() throws Exception {
        String authHeader = bearerFor("admin", "ROLE_ADMIN");

        Profile bob = Profile.builder()
                .username("bob")
                .fullName("Bob")
                .email("bob@mail.com")
                .build();
        profileRepository.save(bob);

        mockMvc.perform(delete(BASE + "/bob").header("Authorization", authHeader))
                .andExpect(status().isNoContent());

        assertTrue(profileRepository.findByUsername("bob").isEmpty());
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
        String token = "bad.jwt.token";
        given(jwtService.isSignatureAndExpiryValid(eq(token))).willReturn(false);

        mockMvc.perform(get(BASE)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
