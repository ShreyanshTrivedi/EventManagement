package com.campus.event.web;

import com.campus.event.domain.Booking;
import com.campus.event.domain.User;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Direct room booking endpoint (faculty meetings — not event-based).
 *
 * Restricted to FACULTY and ADMIN only. GENERAL_USER and CLUB_ASSOCIATE must
 * go through the room-booking-request + admin-approval workflow instead.
 */
@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasAnyRole('FACULTY', 'ADMIN')")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String purpose,
            @AuthenticationPrincipal UserDetails principal
    ) {
        Long targetResourceId = resourceId != null ? resourceId : roomId;
        if (targetResourceId == null) {
            return ResponseEntity.badRequest().body("resourceId or roomId is required");
        }
        // Resolve a real, persisted User entity — NOT a detached stub
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in database"));

        Booking saved = bookingService.createBooking(targetResourceId, user, start, end, purpose);
        return ResponseEntity.ok(saved.getId());
    }
}
