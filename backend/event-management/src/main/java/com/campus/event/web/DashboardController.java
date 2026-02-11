package com.campus.event.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> admin(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(Map.of(
                "role", "ADMIN",
                "user", principal.getUsername()
        ));
    }

    @GetMapping("/faculty")
    @PreAuthorize("hasRole('FACULTY')")
    public ResponseEntity<?> faculty(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(Map.of(
                "role", "FACULTY",
                "user", principal.getUsername()
        ));
    }

    @GetMapping("/club")
    @PreAuthorize("hasRole('CLUB_ASSOCIATE')")
    public ResponseEntity<?> club(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(Map.of(
                "role", "CLUB_ASSOCIATE",
                "user", principal.getUsername()
        ));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('GENERAL_USER')")
    public ResponseEntity<?> user(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(Map.of(
                "role", "GENERAL_USER",
                "user", principal.getUsername()
        ));
    }
}
