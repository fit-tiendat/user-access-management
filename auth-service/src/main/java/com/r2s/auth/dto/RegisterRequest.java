package com.r2s.auth.dto;


import com.r2s.auth.validation.StrongPassword;
import com.r2s.core.entity.Role;
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
    @Pattern(regexp = "^\\S+$", message = "Username must not contain spaces")
    @Size(min = 4, max = 30)
    private String username;

    @NotBlank
    @StrongPassword
    private String password;

    private Role role ;// USER, ADMIN, MODERATOR

}
