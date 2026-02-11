package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> getPublicEvents() {
        return eventRepository.findByIsPublicTrue();
    }

    public Event createEvent(String title, String description, LocalDateTime start, LocalDateTime end,
                              User creator, String location, String clubId, String registrationSchema) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setCreatedBy(creator);
        event.setPublic(true);
        event.setLocation(location != null && !location.isBlank() ? location : "TBD");
        if (clubId != null && !clubId.isBlank()) event.setClubId(clubId);
        else if (creator != null) event.setClubId(creator.getClubId());
        event.setRegistrationSchema(registrationSchema);
        return eventRepository.save(event);
    }
}
