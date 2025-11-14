package com.r2s.user.service;

import com.r2s.core.exception.NotFoundException;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository repo;

    @InjectMocks
    private ProfileServiceImpl service;

    @Test
    void upsert_shouldCreateNewProfileWhenNotExists() {
        var dto = new ProfileDto("john","John","john@ex.com");
        when(repo.findByUsername("john")).thenReturn(Optional.empty());
        when(repo.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        Profile p = service.upsert(dto);

        assertThat(p.getUsername()).isEqualTo("john");
        assertThat(p.getFullName()).isEqualTo("John");
        assertThat(p.getEmail()).isEqualTo("john@ex.com");
        verify(repo).save(any(Profile.class));
    }

    @Test
    void upsert_shouldUpdateExistingProfile() {
        var existing = Profile.builder().id(1L).username("john").fullName("Old").email("old@ex.com").build();
        when(repo.findByUsername("john")).thenReturn(Optional.of(existing));
        when(repo.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = new ProfileDto("john","New","new@ex.com");
        Profile p = service.upsert(dto);

        assertThat(p.getFullName()).isEqualTo("New");
        assertThat(p.getEmail()).isEqualTo("new@ex.com");
        verify(repo).save(existing);
    }

    @Test
    void getByUsername_shouldReturnProfile() {
        var existing = Profile.builder().username("john").build();
        when(repo.findByUsername("john")).thenReturn(Optional.of(existing));

        Profile p = service.getByUsername("john");

        assertThat(p.getUsername()).isEqualTo("john");
    }

    @Test
    void getByUsername_shouldThrowIfNotFound() {
        when(repo.findByUsername("missing")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getByUsername("missing"));
    }

    @Test
    void getAll_shouldReturnAllProfiles() {
        when(repo.findAll()).thenReturn(List.of(new Profile(), new Profile()));
        assertThat(service.getAll()).hasSize(2);
        verify(repo).findAll();
    }

    @Test
    void deleteByUsername_shouldDeleteWhenExists() {
        // nếu repo.deleteByUsername trả về int/long → mock đúng kiểu
        when(repo.deleteByUsername("john")).thenReturn(1); // hoặc 1L
        service.deleteByUsername("john");
        verify(repo).deleteByUsername("john");
    }

    @Test
    void deleteByUsername_shouldThrow404WhenNotFound() {
        when(repo.deleteByUsername("missing")).thenReturn(0); // hoặc 0L
        assertThrows(NotFoundException.class, () -> service.deleteByUsername("missing"));
        verify(repo).deleteByUsername("missing");
    }
}
