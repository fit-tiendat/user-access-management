package com.r2s.user.controller;

import com.r2s.core.dto.ApiResponse;
import com.r2s.core.utils.ResponseBuilder;
import com.r2s.core.utils.ValidationPatterns;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.dto.ProfileResponse;
import com.r2s.user.entity.Profile;
import com.r2s.user.service.ProfileCommandService;
import com.r2s.user.service.ProfileQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final ProfileCommandService commandService;
    private final ProfileQueryService queryService;

    private final @Qualifier("r2sResponseBuilder") ResponseBuilder responseBuilder;

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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> me(Principal principal) {
        String username = requireUsername(principal);
        Profile profile = queryService.getByUsername(username);
        return responseBuilder.ok(toResponse(profile), "Profile retrieved successfully");
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> upsertMe(
            Principal principal,
            @Valid @RequestBody ProfileDto dto
    ) {
        String username = requireUsername(principal);

        Profile saved = commandService.upsert(username, dto);
        return responseBuilder.ok(toResponse(saved), "Profile updated successfully");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> all() {
        List<ProfileResponse> result = queryService.getAll()
                .stream()
                .map(this::toResponse)
                .toList();

        return responseBuilder.ok(result, "Profiles retrieved successfully");
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable("username")
            @Pattern(
                    regexp = ValidationPatterns.USERNAME,
                    message = ValidationPatterns.USERNAME_MESSAGE
            )
            @Size(min = 4, max = 30)
            String username
    ) {
        commandService.deleteByUsername(username);
        return ResponseEntity.noContent().build();
    }
}
