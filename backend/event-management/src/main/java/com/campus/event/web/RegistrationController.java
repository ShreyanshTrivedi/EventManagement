package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.Registration;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.RegistrationService;
import com.campus.event.web.dto.RegisterEventRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RegistrationRepository registrationRepository;

    public RegistrationController(RegistrationService registrationService, EventRepository eventRepository, UserRepository userRepository, RegistrationRepository registrationRepository) {
        this.registrationService = registrationService;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE')")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterEventRequest body,
                                      @org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails principal) {
        try {
            Event event = eventRepository.findById(body.getEventId()).orElse(null);
            if (event == null) return ResponseEntity.badRequest().body("Event not found");

            User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            Set<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_FACULTY")) {
                return ResponseEntity.status(403).body("Admins and Faculty cannot register for events");
            }

            if (roles.contains("ROLE_CLUB_ASSOCIATE")) {
                if (user != null && event.getClubId() != null && event.getClubId().equals(user.getClubId())) {
                    return ResponseEntity.status(403).body("Club Associates cannot register for their own club events");
                }
            }

            // Always register using the authenticated user's account email so that
            // /api/registrations/mine (which looks up by user.email) will include this registration.
            String effectiveEmail = user.getEmail();
            Registration reg = registrationService.registerForEvent(body.getEventId(), effectiveEmail, body.getFullName());
            Map<String, Object> answers = body.getAnswers();
            if (answers != null && !answers.isEmpty()) {
                reg.setAnswersJson(objectMapper.writeValueAsString(answers));
            }
            return ResponseEntity.ok("Registration successful");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> myRegistrations(@org.springframework.security.core.annotation.AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        List<Registration> regs = registrationRepository.findByEmail(user.getEmail());
        List<Map<String, Object>> body = regs.stream().map(r -> {
            Event ev = r.getEvent();
            java.util.HashMap<String, Object> m = new java.util.HashMap<>();
            m.put("eventId", ev != null ? ev.getId() : null);
            m.put("title", ev != null ? ev.getTitle() : null);
            m.put("startTime", ev != null ? ev.getStartTime() : null);
            m.put("endTime", ev != null ? ev.getEndTime() : null);
            m.put("location", ev != null ? ev.getLocation() : null);
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
