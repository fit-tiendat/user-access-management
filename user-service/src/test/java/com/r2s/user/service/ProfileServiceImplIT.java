package com.r2s.user.service;

import com.r2s.core.exception.NotFoundException;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.repository.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Testcontainers
class ProfileServiceImplIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("user_access_management")
                    .withUsername("postgres")
                    .withPassword("d433221dat");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired ProfileService profileService;
    @Autowired ProfileRepository profileRepository;

    @Test
    @DisplayName("upsert: chưa có profile -> tạo mới")
    void upsert_creates_profile_when_not_exists() {
        // given
        String username = "alice_it";
        ProfileDto dto = new ProfileDto(
                "Alice Integration",
                "alice.it@mail.com"
        );

        // when
        Profile saved = profileService.upsert(username, dto);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo(username);
        assertThat(saved.getFullName()).isEqualTo("Alice Integration");
        assertThat(saved.getEmail()).isEqualTo("alice.it@mail.com");

        // kiểm tra DB thực sự có record
        Profile inDb = profileRepository.findByUsername(username).orElseThrow();
        assertThat(inDb.getFullName()).isEqualTo("Alice Integration");
        assertThat(inDb.getEmail()).isEqualTo("alice.it@mail.com");
    }

    @Test
    @DisplayName("upsert: đã có profile -> update fullName & email")
    void upsert_updates_existing_profile() {
        // given: profile có sẵn
        String username = "bob_it";
        Profile existing = Profile.builder()
                .username(username)
                .fullName("Bob Old")
                .email("old@mail.com")
                .build();
        profileRepository.save(existing);

        ProfileDto dto = new ProfileDto(
                "Bob New",
                "new@mail.com"
        );

        // when
        Profile updated = profileService.upsert(username, dto);

        // then
        assertThat(updated.getUsername()).isEqualTo(username);
        assertThat(updated.getFullName()).isEqualTo("Bob New");
        assertThat(updated.getEmail()).isEqualTo("new@mail.com");

        Profile inDb = profileRepository.findByUsername(username).orElseThrow();
        assertThat(inDb.getFullName()).isEqualTo("Bob New");
        assertThat(inDb.getEmail()).isEqualTo("new@mail.com");
    }

    @Test
    @DisplayName("getByUsername: không tìm thấy -> NotFoundException")
    void getByUsername_throws_when_not_found() {
        assertThrows(NotFoundException.class,
                () -> profileService.getByUsername("missing-user"));
    }

    @Test
    @DisplayName("getByUsername: tìm thấy -> trả về Profile")
    void getByUsername_returns_profile_when_found() {
        // given
        String username = "charlie_it";
        Profile existing = Profile.builder()
                .username(username)
                .fullName("Charlie Integration")
                .email("charlie.it@mail.com")
                .build();
        profileRepository.save(existing);

        // when
        Profile found = profileService.getByUsername(username);

        // then
        assertThat(found.getId()).isNotNull();
        assertThat(found.getUsername()).isEqualTo(username);
        assertThat(found.getFullName()).isEqualTo("Charlie Integration");
        assertThat(found.getEmail()).isEqualTo("charlie.it@mail.com");
    }

    @Test
    @DisplayName("deleteByUsername: tồn tại -> xóa thành công")
    void deleteByUsername_deletes_when_exists() {
        // given
        String username = "will_delete";
        Profile existing = Profile.builder()
                .username(username)
                .fullName("To Delete")
                .email("delete@mail.com")
                .build();
        profileRepository.save(existing);

        // sanity check
        assertThat(profileRepository.findByUsername(username)).isPresent();

        // when
        profileService.deleteByUsername(username);

        // then
        assertThat(profileRepository.findByUsername(username)).isEmpty();
    }

    @Test
    @DisplayName("deleteByUsername: không tồn tại -> NotFoundException")
    void deleteByUsername_throws_when_not_exists() {
        assertThrows(NotFoundException.class,
                () -> profileService.deleteByUsername("missing-user"));
    }
}
