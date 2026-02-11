package com.campus.event.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @Email
    @NotBlank
    private String email;
    private String role;
        // Explicit getters
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
}
