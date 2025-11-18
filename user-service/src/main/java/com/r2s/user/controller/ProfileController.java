package com.r2s.user.controller;// import thêm:
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.dto.ProfileResponse;
import com.r2s.user.entity.Profile;
import com.r2s.user.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Validated
@RestController
@RequestMapping("${api.base-path:/api/v1}/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;

    // ===== helper =====
    private String requireUsername(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalStateException("Authentication is missing");
        }
        return principal.getName();
    }

    private ProfileResponse toResponse(Profile p) {
        return new ProfileResponse(
                p.getId(),
                p.getUsername(),
                p.getFullName(),
                p.getEmail()
        );
    }

    // ===== API =====

    // user tự xem profile của mình
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> me(Principal principal) {
        String username = requireUsername(principal);
        Profile profile = service.getByUsername(username);
        return ResponseEntity.ok(toResponse(profile));
    }

    // user tự cập nhật profile của mình
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> upsertMe(Principal principal,
                                                    @Valid @RequestBody ProfileDto dto) {
        String username = requireUsername(principal);

        // ép username theo token, bỏ qua username client gửi lên
        Profile saved = service.upsert(
                new ProfileDto(username, dto.fullName(), dto.email())
        );
        return ResponseEntity.ok(toResponse(saved));
    }

    // ADMIN xem tất cả profile
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfileResponse>> all() {
        List<ProfileResponse> result = service.getAll()
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    // ADMIN xóa theo username
    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable("username")
            @Pattern(regexp = "^\\S+$", message = "Username must not contain spaces")
            String username) {

        service.deleteByUsername(username);
        return ResponseEntity.noContent().build();
    }
}
