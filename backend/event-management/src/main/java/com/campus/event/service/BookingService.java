package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.Room;
import com.campus.event.domain.User;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    public BookingService(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public Booking createBooking(Long roomId, User user, LocalDateTime start, LocalDateTime end, String purpose) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("Room not found"));

        List<Booking> conflicts = bookingRepository
                .findByRoomIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(roomId, end, start);
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Room is already booked for the selected time range");
        }

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setPurpose(purpose);
        return bookingRepository.save(booking);
    }
}


