package com.campus.event.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username;
    @NotBlank
    private String password;
        // Explicit getters
        public String getUsername() { return username; }
        public String getPassword() { return password; }
}


