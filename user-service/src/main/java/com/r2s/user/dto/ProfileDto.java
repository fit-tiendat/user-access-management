package com.r2s.user.dto;

import com.r2s.core.utils.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileDto(

        @NotBlank
        @Size(max = 100)
        @Pattern(
                regexp = ValidationPatterns.DISPLAY_NAME,
                message = ValidationPatterns.DISPLAY_NAME_MESSAGE
        )
        String fullName,

        @NotBlank
        @Email
        @Size(max = 100)
        String email
) {
}
