package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceType;
import com.campus.event.domain.User;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.ResourceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;

    public BookingService(BookingRepository bookingRepository, ResourceRepository resourceRepository) {
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public Booking createBooking(Long resourceId, User user, LocalDateTime start, LocalDateTime end, String purpose) {
        Resource resource = resourceRepository.findById(resourceId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        if (resource.getResourceType() == ResourceType.OPEN_SPACE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot book an OPEN_SPACE for a standard meeting.");
        }

        List<Booking> conflicts = bookingRepository
                .findOverlappingByResource(resourceId, start, end);
        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Resource already booked for selected time");
        }

        Booking booking = new Booking();
        booking.setResource(resource);
        booking.setUser(user);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setPurpose(purpose);
        return bookingRepository.save(booking);
    }
}


