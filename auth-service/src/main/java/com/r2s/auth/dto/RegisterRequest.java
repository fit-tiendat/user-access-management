package com.r2s.auth.dto;


import com.r2s.auth.validation.StrongPassword;
import com.r2s.core.utils.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterRequest {
    @NotBlank
    @Pattern(
            regexp = ValidationPatterns.USERNAME,
            message = ValidationPatterns.USERNAME_MESSAGE
    )
    @Size(min = 4, max = 30)
    private String username;

    @NotBlank
    @StrongPassword
    private String password;
}
