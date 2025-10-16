package com.r2s.user.controller;

import com.r2s.user.dto.ProfileDto;
import com.r2s.user.entity.Profile;
import com.r2s.user.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.base-path:/api/v1}/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    // user tự xem/ghi profile của mình
    @GetMapping("/me")
    public ResponseEntity<Profile> me(Authentication auth) {
        return ResponseEntity.ok(service.getByUsername(auth.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<Profile> upsertMe(Authentication auth, @Valid @RequestBody ProfileDto dto) {
        // ép username theo token để tránh ghi profile của người khác
        Profile enforced = service.upsert(new ProfileDto(auth.getName(), dto.fullName(), dto.email()));
        return ResponseEntity.ok(enforced);
    }

    // ADMIN xem tất cả hoặc xóa theo username
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Profile>> all() {
        return ResponseEntity.ok(service.getAll());
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("username") String username) {
        service.deleteByUsername(username);
        return ResponseEntity.noContent().build();
    }
}
