package com.r2s.auth.dto;

import com.r2s.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 4, max = 30)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

//    @NotBlank @Email
//    private String email;
//
//    @NotBlank
//    private String fullName;
//
//    // Optional: allow choosing role; default USER if null
//    private Role role;
}