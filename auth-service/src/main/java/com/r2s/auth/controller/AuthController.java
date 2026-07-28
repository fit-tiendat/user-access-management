package com.r2s.auth.controller;

import com.r2s.core.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.service.AuthenticationService;
import com.r2s.auth.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.r2s.core.config.OpenApiConfig.BEARER_AUTH;

@RestController
@RequestMapping("${api.base-path:/api/v1}/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, and authenticated access checks")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Register a user", description = "Creates a USER account after validating the credentials")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Username already exists"),
            @ApiResponse(responseCode = "429", description = "Registration rate limit exceeded")
    })
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Authenticates credentials and returns a signed JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication succeeded"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Credentials are invalid"),
            @ApiResponse(responseCode = "429", description = "Login rate limit exceeded")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check administrator access")
    @SecurityRequirement(name = BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Administrator access granted"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Administrator role is required")
    })
    public ResponseEntity<String> adminOnly() {
        return ResponseEntity.ok("ADMIN area");
    }
}
