package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventRegistration;
import com.campus.event.domain.EventStatus;
import com.campus.event.domain.NotificationDelivery;
import com.campus.event.domain.NotificationMessage;
import com.campus.event.domain.NotificationStatus;
import com.campus.event.domain.Registration;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.NotificationDeliveryRepository;
import com.campus.event.repository.NotificationMessageRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.NotificationService;
import com.campus.event.service.WaitlistService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventRegistrationController {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final RegistrationRepository legacyRegistrationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationMessageRepository notificationMessageRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final WaitlistService waitlistService;

    public EventRegistrationController(EventRepository eventRepository,
                                       EventRegistrationRepository registrationRepository,
                                       RegistrationRepository legacyRegistrationRepository,
                                       UserRepository userRepository,
                                       NotificationService notificationService,
                                       NotificationMessageRepository notificationMessageRepository,
                                       NotificationDeliveryRepository notificationDeliveryRepository,
                                       WaitlistService waitlistService) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.legacyRegistrationRepository = legacyRegistrationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.notificationMessageRepository = notificationMessageRepository;
        this.notificationDeliveryRepository = notificationDeliveryRepository;
        this.waitlistService = waitlistService;
    }

    // =========================================================================
    // POST /api/events/{eventId}/register — register or join waitlist
    // =========================================================================

    @PostMapping("/events/{eventId}/register")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','ADMIN')")
    @Transactional
    public ResponseEntity<?> register(@PathVariable Long eventId,
                                      @AuthenticationPrincipal UserDetails principal,
                                      @Valid @RequestBody(required = false) Map<String, Object> payload) {

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        // ── Duplicate guard ──────────────────────────────────────────────────
        if (registrationRepository.existsByEvent_IdAndUser_Username(eventId, principal.getUsername())) {
            return ResponseEntity.ok(Map.of("status", "already_registered"));
        }

        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // ── Role checks ──────────────────────────────────────────────────────
        Set<String> roles = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        if (roles.contains("ROLE_ADMIN")
                || roles.contains("ROLE_FACULTY")
                || roles.contains("ROLE_BUILDING_ADMIN")
                || roles.contains("ROLE_CENTRAL_ADMIN")) {
            return ResponseEntity.status(403).body("Admins and Faculty cannot register for events");
        }

        // ── Club associate cannot register for their own club's event ─────────
        if (roles.contains("ROLE_CLUB_ASSOCIATE") && user.getClubId() != null
                && event.getClubId() != null && user.getClubId().equals(event.getClubId())) {
            return ResponseEntity.status(403).body("Club Associates cannot register for their own club events");
        }

        // ── Creator cannot register for their own event ───────────────────────
        if (event.getCreatedBy() != null
                && event.getCreatedBy().getUsername().equals(user.getUsername())) {
            return ResponseEntity.status(403).body("Event creators cannot register for their own events");
        }

        // ── Registration deadline ─────────────────────────────────────────────
        if (event.getStartTime() != null) {
            if (event.getStartTime().isBefore(LocalDateTime.now().plusSeconds(1))) {
                return ResponseEntity.status(403).body("Cannot register for past events");
            }
            if (LocalDateTime.now().isAfter(event.getStartTime().minusDays(2))) {
                return ResponseEntity.status(403).body("Registration closed 2 days before event start");
            }
        }

        // ── Capacity check — pessimistic lock ────────────────────────────────
        Event lockedEvent = eventRepository.findByIdWithLock(eventId).orElse(null);
        if (lockedEvent == null) return ResponseEntity.notFound().build();

        if (lockedEvent.getMaxAttendees() != null && lockedEvent.getMaxAttendees() > 0) {
            long currentCount = registrationRepository.countByEvent_Id(eventId);
            if (currentCount >= lockedEvent.getMaxAttendees()) {
                // ── Event is full → join waitlist ────────────────────────────
                if (waitlistService.getPosition(eventId, user.getUsername()).isPresent()) {
                    int pos = waitlistService.getPosition(eventId, user.getUsername()).get();
                    return ResponseEntity.status(202)
                            .body(Map.of("status", "WAITLISTED", "position", pos));
                }
                int position = waitlistService.joinWaitlist(event, user);
                return ResponseEntity.status(202)
                        .body(Map.of("status", "WAITLISTED", "position", position,
                                "message", "Event is full. You have been added to the waitlist at position " + position + "."));
            }
        }

        // ── Create registration ───────────────────────────────────────────────
        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        registrationRepository.save(reg);

        // Backfill existing event notifications to late registrants
        try {
            if (user.getId() != null) {
                List<NotificationMessage> existingMessages =
                        notificationMessageRepository.findByEvent_IdOrderByCreatedAtDesc(eventId);
                List<Long> alreadyDelivered =
                        notificationDeliveryRepository.findDeliveredNotificationIdsForUserEvent(user.getId(), eventId);
                HashSet<Long> alreadySet = new HashSet<>(alreadyDelivered);
                List<NotificationDelivery> toCreate = new ArrayList<>();
                for (NotificationMessage nm : existingMessages) {
                    if (nm == null || nm.getId() == null || alreadySet.contains(nm.getId())) continue;
                    NotificationDelivery d = new NotificationDelivery();
                    d.setNotification(nm);
                    d.setUser(user);
                    d.setDeliveryStatus(NotificationStatus.PENDING);
                    toCreate.add(d);
                }
                if (!toCreate.isEmpty()) notificationDeliveryRepository.saveAll(toCreate);
            }
        } catch (Exception ignored) { /* Best-effort */ }

        // Sync with legacy registrations table
        try {
            String email = user.getEmail();
            if (email != null && !legacyRegistrationRepository.existsByEventIdAndEmail(eventId, email)) {
                Registration legacy = new Registration();
                legacy.setEvent(event);
                legacy.setEmail(email);
                Object fullName = payload != null ? payload.get("fullName") : null;
                legacy.setFullName(fullName != null ? String.valueOf(fullName) : user.getUsername());
                legacyRegistrationRepository.save(legacy);
            }
        } catch (Exception ignored) { /* Best-effort */ }

        notificationService.notifyAllChannels(user,
                "Registration confirmed: " + event.getTitle(),
                "You are registered for '" + event.getTitle() + "' starting at " + event.getStartTime() + ".");

        return ResponseEntity.ok(Map.of("registrationId", reg.getId()));
    }

    // =========================================================================
    // DELETE /api/events/{eventId}/register — cancel registration (unregister)
    // =========================================================================

    @DeleteMapping("/events/{eventId}/register")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','ADMIN')")
    @Transactional
    public ResponseEntity<?> unregister(@PathVariable Long eventId,
                                        @AuthenticationPrincipal UserDetails principal) {

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return ResponseEntity.notFound().build();

        // Block cancellation after the event has ended
        if (event.getStatus() == EventStatus.COMPLETED) {
            return ResponseEntity.status(400).body("Cannot cancel registration for a completed event");
        }

        // Block cancellation within 2 days of event start
        if (event.getStartTime() != null
                && LocalDateTime.now().isAfter(event.getStartTime().minusDays(2))) {
            return ResponseEntity.status(400).body("Cannot cancel registration within 2 days of event start");
        }

        User user = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // Find and delete from event_registrations
        EventRegistration reg = registrationRepository
                .findByEvent_IdAndUser_Username(eventId, principal.getUsername())
                .orElse(null);
        if (reg == null) {
            // Maybe they're on the waitlist
            if (waitlistService.getPosition(eventId, principal.getUsername()).isPresent()) {
                waitlistService.leaveWaitlist(eventId, principal.getUsername());
                return ResponseEntity.ok(Map.of("status", "removed_from_waitlist"));
            }
            return ResponseEntity.status(404).body("Registration not found");
        }

        registrationRepository.delete(reg);

        // Clean up legacy registrations table
        try {
            if (user.getEmail() != null) {
                legacyRegistrationRepository.deleteByEventIdAndEmail(eventId, user.getEmail());
            }
        } catch (Exception ignored) { /* Best-effort */ }

        // Remove from waitlist if they were also queued somehow
        waitlistService.leaveWaitlist(eventId, principal.getUsername());

        // Promote the next person on the waitlist
        waitlistService.promoteNext(eventId);

        notificationService.notifyAllChannels(user,
                "Registration cancelled: " + event.getTitle(),
                "Your registration for '" + event.getTitle() + "' has been cancelled.");

        return ResponseEntity.ok(Map.of("status", "cancelled"));
    }

    // =========================================================================
    // GET /api/event-registrations/mine
    // =========================================================================

    @GetMapping("/event-registrations/mine")
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> myRegistrations(
            @AuthenticationPrincipal UserDetails principal) {
        List<EventRegistration> regs =
                registrationRepository.findByUserUsernameWithEvent(principal.getUsername());
        List<Map<String, Object>> body = regs.stream().map(r -> {
            HashMap<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("eventId", r.getEvent() != null ? r.getEvent().getId() : null);
            m.put("title", r.getEvent() != null ? r.getEvent().getTitle() : null);
            m.put("startTime", r.getEvent() != null ? r.getEvent().getStartTime() : null);
            m.put("endTime", r.getEvent() != null ? r.getEvent().getEndTime() : null);
            m.put("location", r.getEvent() != null ? r.getEvent().getLocation() : null);
            m.put("status", r.getEvent() != null && r.getEvent().getStatus() != null
                    ? r.getEvent().getStatus().name() : null);
            m.put("registeredAt", r.getRegisteredAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
