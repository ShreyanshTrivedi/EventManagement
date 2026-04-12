package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.Event;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.FixedTimetableRepository;
import com.campus.event.repository.ResourceRepository;
import com.campus.event.repository.ResourceBookingRequestRepository;
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
    private final BookingRepository bookingRepository;
    private final ResourceRepository resourceRepository;

    @Autowired
    public RoomAvailabilityService(ResourceBookingRequestRepository requestRepo,
                                   FixedTimetableRepository fixedTimetableRepository,
                                   BookingRepository bookingRepository,
                                   ResourceRepository resourceRepository) {
        this.requestRepo = requestRepo;
        this.fixedTimetableRepository = fixedTimetableRepository;
        this.bookingRepository = bookingRepository;
        this.resourceRepository = resourceRepository;
    }

    public boolean isResourceAvailable(Long resourceId, LocalDateTime start, LocalDateTime end) {
        Resource resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null) return false;

        if (hasFixedTimetableConflict(resourceId, start, end)) return false;
        if (hasEventBookingConflict(resourceId, start, end)) return false;

        return bookingRepository.findOverlappingByResource(resourceId, start, end).isEmpty();
    }

    public Map<Long, Boolean> availabilityForResources(List<Long> resourceIds,
                                                        LocalDateTime start,
                                                        LocalDateTime end) {
        return resourceIds.stream().collect(Collectors.toMap(
                id -> id, id -> isResourceAvailable(id, start, end)));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean hasEventBookingConflict(Long resourceId, LocalDateTime start, LocalDateTime end) {
        List<ResourceBookingRequest> existing = requestRepo.findByStatusIn(
                Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return existing.stream()
                .filter(b -> b.getAllocatedResource() != null
                        && b.getAllocatedResource().getId().equals(resourceId))
                .anyMatch(b -> overlaps(windowStart(b), windowEnd(b), start, end));
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
