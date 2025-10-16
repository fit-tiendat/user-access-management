package com.r2s.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ProfileDto(
        @NotBlank String username,
        String fullName,
        String email
) {
}
