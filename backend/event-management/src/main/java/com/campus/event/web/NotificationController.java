package com.campus.event.web;

import com.campus.event.domain.Notification;
import com.campus.event.repository.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> mine(@AuthenticationPrincipal UserDetails principal) {
        List<Notification> items = notificationRepository.findByUser_UsernameOrderByCreatedAtDesc(principal.getUsername());
        List<Map<String, Object>> body = items.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType().name());
            m.put("status", n.getStatus().name());
            m.put("subject", n.getSubject());
            m.put("message", n.getMessage());
            m.put("createdAt", n.getCreatedAt());
            m.put("sentAt", n.getSentAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
