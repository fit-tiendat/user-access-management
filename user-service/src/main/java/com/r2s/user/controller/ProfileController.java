package com.r2s.user.controller;

import com.r2s.core.dto.ApiResponse;
import com.r2s.core.dto.PageResponse;
import com.r2s.core.utils.ResponseBuilder;
import com.r2s.core.utils.ValidationPatterns;
import com.r2s.user.dto.ProfileDto;
import com.r2s.user.dto.ProfileResponse;
import com.r2s.user.entity.Profile;
import com.r2s.user.service.ProfileCommandService;
import com.r2s.user.service.ProfileQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static com.r2s.core.config.OpenApiConfig.BEARER_AUTH;

@Validated
@RestController
@RequestMapping("${api.base-path:/api/v1}/users")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Authenticated profile management")
@SecurityRequirement(name = BEARER_AUTH)
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
    @Operation(summary = "Get the current user's profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile does not exist")
    })
    public ResponseEntity<ApiResponse<ProfileResponse>> me(Principal principal) {
        String username = requireUsername(principal);
        Profile profile = queryService.getByUsername(username);
        return responseBuilder.ok(toResponse(profile), "Profile retrieved successfully");
    }

    @PutMapping("/me")
    @Operation(summary = "Create or update the current user's profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile saved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Request validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email is already in use")
    })
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
    @Operation(summary = "List profiles", description = "Returns a username-sorted page of profiles")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile page returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Pagination parameters are invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Administrator role is required")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProfileResponse>>> all(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<ProfileResponse> result = queryService.getAll(
                        PageRequest.of(page, size, Sort.by("username").ascending())
                )
                .map(this::toResponse);

        return responseBuilder.ok(PageResponse.from(result), "Profiles retrieved successfully");
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Profile deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Username is invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Administrator role is required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile does not exist")
    })
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
