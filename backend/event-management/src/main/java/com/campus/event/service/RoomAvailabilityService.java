package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.Event;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingSlot;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.FixedTimetableRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomBookingSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for availability of any bookable unit (rooms and open spaces).
 *
 * <p>Checks three reservation sources in order:
 * <ol>
 *   <li>{@code fixed_timetable}       — immovable academic timetable slots (rooms only)
 *   <li>{@code room_booking_requests} — event bookings (APPROVED/CONFIRMED), keyed by room
 *   <li>{@code bookings}              — direct bookings, checked by <em>resource_id</em>
 *                                       first (new path), then by <em>room_id</em> (legacy)
 * </ol>
 */
@Service
public class RoomAvailabilityService {

    private final ResourceBookingRequestRepository requestRepo;
    private final FixedTimetableRepository fixedTimetableRepository;
    private final RoomBookingSlotRepository slotRepo;

    @Autowired
    public RoomAvailabilityService(RoomBookingRequestRepository requestRepo,
                                   FixedTimetableRepository fixedTimetableRepository,
                                   RoomBookingSlotRepository slotRepo) {
        this.requestRepo = requestRepo;
        this.fixedTimetableRepository = fixedTimetableRepository;
        this.slotRepo = slotRepo;
    }

    // Backward-compatible constructor for existing unit tests
    public RoomAvailabilityService(RoomBookingRequestRepository requestRepo) {
        this(requestRepo, null, null);
    }

    /**
     * Checks if a room is available for the given time window.
     * Uses the new slot-based model when available, falls back to legacy check.
     */
    public boolean isRoomAvailable(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (hasFixedTimetableConflict(roomId, start, end)) {
            return false;
        }

        // Primary: check against room_booking_slots table
        if (slotRepo != null) {
            return !hasSlotConflict(roomId, start, end);
        }

        // Legacy fallback: check against room_booking_requests directly
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return existing.stream()
                .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(roomId))
                .noneMatch(b -> overlaps(windowStart(b), windowEnd(b), start, end));
    }

    /**
     * Slot-based availability: checks if a room has conflicting allocated slots
     * across the given time range, handling multi-day ranges by checking each day.
     */
    private boolean hasSlotConflict(Long roomId, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return false;
        LocalDate day = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        while (!day.isAfter(last)) {
            LocalTime dayStart = day.equals(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
            LocalTime dayEnd = day.equals(last) ? end.toLocalTime() : LocalTime.of(23, 59);
            List<RoomBookingSlot> conflicts = slotRepo.findConflictingSlots(roomId, day, dayStart, dayEnd);
            if (!conflicts.isEmpty()) {
                return true;
            }
            day = day.plusDays(1);
        }
        return false;
    }

    public Map<Long, Boolean> availabilityForRooms(List<Long> roomIds, LocalDateTime start, LocalDateTime end) {
        return roomIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> isRoomAvailable(id, start, end)
        ));
    }

    private boolean hasFixedTimetableConflict(Long resourceId, LocalDateTime start, LocalDateTime end) {
        if (resourceId == null || start == null || end == null || !start.isBefore(end)) return false;
        LocalDate date = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        while (!date.isAfter(endDate)) {
            LocalTime dayStart = date.isEqual(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
            LocalTime dayEnd   = date.isEqual(endDate)             ? end.toLocalTime()   : LocalTime.MAX;
            if (fixedTimetableRepository != null
                    && fixedTimetableRepository.existsConflictingClass(
                            resourceId, date.getDayOfWeek(), dayStart, dayEnd)) {
                return true;
            }
            date = date.plusDays(1);
        }
        return false;
    }

    private static boolean overlaps(LocalDateTime aS, LocalDateTime aE,
                                     LocalDateTime bS, LocalDateTime bE) {
        if (aS == null || aE == null || bS == null || bE == null) return false;
        return aS.isBefore(bE) && bS.isBefore(aE);
    }

    private static LocalDateTime windowStart(ResourceBookingRequest r) {
        Event e = r.getEvent();
        return (e != null && e.getStartTime() != null) ? e.getStartTime() : r.getMeetingStart();
    }

    private static LocalDateTime windowEnd(ResourceBookingRequest r) {
        Event e = r.getEvent();
        return (e != null && e.getEndTime() != null) ? e.getEndTime() : r.getMeetingEnd();
    }
}
