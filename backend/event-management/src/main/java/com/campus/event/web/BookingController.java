package com.campus.event.web;

import com.campus.event.domain.Booking;
import com.campus.event.domain.User;
import com.campus.event.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String purpose,
            @AuthenticationPrincipal UserDetails principal
    ) {
        // In a full implementation, map principal to User entity; for MVP, use username
        User user = new User();
        user.setUsername(principal.getUsername());
        Booking saved = bookingService.createBooking(roomId, user, start, end, purpose);
        return ResponseEntity.ok(saved.getId());
    }
}


