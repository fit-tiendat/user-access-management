package com.r2s.auth.dto;

import com.r2s.auth.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 4, max = 30)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private Role role ;// USER, ADMIN, MODERATOR

}
