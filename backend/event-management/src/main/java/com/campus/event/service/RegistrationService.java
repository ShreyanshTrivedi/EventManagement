package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.Registration;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;

    public RegistrationService(RegistrationRepository registrationRepository, EventRepository eventRepository) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
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

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setEmail(email);
        registration.setFullName(fullName);
        return registrationRepository.save(registration);
    }
}
