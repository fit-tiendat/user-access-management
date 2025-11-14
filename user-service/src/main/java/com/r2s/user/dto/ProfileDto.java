package com.r2s.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProfileDto(

        @NotBlank
        @Pattern(regexp = "^\\S+$", message = "Username must not contain spaces")
        String username,

        String fullName,

        @NotBlank @Email
        String email
) {
}
