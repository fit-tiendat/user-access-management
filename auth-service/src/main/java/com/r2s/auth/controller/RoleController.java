package com.r2s.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.r2s.core.config.OpenApiConfig.BEARER_AUTH;

@RestController
@RequestMapping("${api.base-path:/api/v1}/role")
@Tag(name = "Role access", description = "Endpoints used to verify role-based authorization")
@SecurityRequirement(name = BEARER_AUTH)
public class RoleController {

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Check USER access")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "USER access granted"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "USER role is required")
    })
    public ResponseEntity<String> userAccess() {
        return ResponseEntity.ok("Hello USER");
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check ADMIN access")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ADMIN access granted"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "ADMIN role is required")
    })
    public ResponseEntity<String> adminAccess() {
        return ResponseEntity.ok("Welcome ADMIN");
    }

    @GetMapping("/mod")
    @PreAuthorize("hasRole('MODERATOR')")
    @Operation(summary = "Check MODERATOR access")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "MODERATOR access granted"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "MODERATOR role is required")
    })
    public ResponseEntity<String> moderatorAccess() {
        return ResponseEntity.ok("Hello MODERATOR");
    }
}
