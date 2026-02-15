package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.Registration;
import com.campus.event.domain.Role;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository, UserRepository userRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Registration registerForEvent(Long eventId, String email, String fullName) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));
        LocalDateTime now = LocalDateTime.now();

        if (event.getStartTime() != null && now.isAfter(event.getStartTime().minusDays(2))) {
            throw new IllegalStateException("Registration closed 2 days before event start");
        }

        if (registrationRepository.existsByEventIdAndEmail(eventId, email)) {
            throw new IllegalStateException("Already registered for this event");
        }

        // Extra safety: prevent Club Associates from registering for events created by their own club
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getRoles() != null) {
                if (user.getRoles().contains(Role.ADMIN) || user.getRoles().contains(Role.FACULTY)) {
                    throw new IllegalStateException("Admins and Faculty cannot register for events");
                }
                if (user.getRoles().contains(Role.CLUB_ASSOCIATE) && user.getClubId() != null && event.getClubId() != null
                        && user.getClubId().equals(event.getClubId())) {
                    throw new IllegalStateException("Club Associates cannot register for their own club events");
                }
                // creators cannot register for their own events (covers when clubId is missing)
                if (event.getCreatedBy() != null && user.getUsername() != null && event.getCreatedBy().getUsername().equals(user.getUsername())) {
                    throw new IllegalStateException("Event creators cannot register for their own events");
                }
            }
        });

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setEmail(email);
        registration.setFullName(fullName);
        return registrationRepository.save(registration);
    }
}
