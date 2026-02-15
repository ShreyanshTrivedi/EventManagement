package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventRegistration;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors; 

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class EventRegistrationController {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public EventRegistrationController(EventRepository eventRepository,
                                       EventRegistrationRepository registrationRepository,
                                       UserRepository userRepository,
                                       NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @PostMapping("/events/{eventId}/register")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<?> register(@PathVariable Long eventId,
                                      @AuthenticationPrincipal UserDetails principal,
                                      @Valid @RequestBody(required = false) Map<String, Object> payload) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        // prevent duplicate
        if (registrationRepository.existsByEvent_IdAndUser_Username(eventId, principal.getUsername())) {
            return ResponseEntity.badRequest().body("Already registered");
        }

        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // role checks: Admins/Faculty cannot register
        Set<String> roles = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_FACULTY")) {
            return ResponseEntity.status(403).body("Admins and Faculty cannot register for events");
        }

        // club associates cannot register for their own club events
        if (roles.contains("ROLE_CLUB_ASSOCIATE") && user.getClubId() != null && event.getClubId() != null
                && user.getClubId().equals(event.getClubId())) {
            return ResponseEntity.status(403).body("Club Associates cannot register for their own club events");
        }

        // creators cannot register for their own events (covers missing clubId cases)
        if (event.getCreatedBy() != null && user.getUsername() != null && event.getCreatedBy().getUsername().equals(user.getUsername())) {
            return ResponseEntity.status(403).body("Event creators cannot register for their own events");
        }

        // registration deadline: closed 2 days before start and disallow past events
        if (event.getStartTime() != null) {
            if (event.getStartTime().isBefore(LocalDateTime.now().plusSeconds(1))) {
                return ResponseEntity.status(403).body("Cannot register for past events");
            }
            if (LocalDateTime.now().isAfter(event.getStartTime().minusDays(2))) {
                return ResponseEntity.status(403).body("Registration closed 2 days before event start");
            }
        }

        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        registrationRepository.save(reg);

        String subject = "Registration confirmed: " + event.getTitle();
        String msg = "You are registered for '" + event.getTitle() + "' starting at " + event.getStartTime() + ".";
        notificationService.notifyAllChannels(user, subject, msg);
        return ResponseEntity.ok(Map.of("registrationId", reg.getId()));
    }

    @GetMapping("/event-registrations/mine")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> myRegistrations(@AuthenticationPrincipal UserDetails principal) {
        List<EventRegistration> regs = registrationRepository.findByUser_Username(principal.getUsername());
        List<Map<String, Object>> body = regs.stream().map(r -> {
            HashMap<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("eventId", r.getEvent() != null ? r.getEvent().getId() : null);
            m.put("eventTitle", r.getEvent() != null ? r.getEvent().getTitle() : null);
            m.put("registeredAt", r.getRegisteredAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
