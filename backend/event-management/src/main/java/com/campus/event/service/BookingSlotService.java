package com.campus.event.service;

import com.campus.event.domain.*;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.RoomBookingSlotRepository;
import com.campus.event.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Central service for the slot-based booking model.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate {@link RoomBookingSlot} rows when a booking is created</li>
 *   <li>Handle overnight events by splitting at midnight</li>
 *   <li>Validate availability per-slot</li>
 *   <li>Allocate rooms per-slot (bulk or individual)</li>
 * </ul>
 */
@Service
public class BookingSlotService {

    private static final Logger log = LoggerFactory.getLogger(BookingSlotService.class);

    private final RoomBookingSlotRepository slotRepo;
    private final EventTimeSlotRepository eventTimeSlotRepo;
    private final RoomRepository roomRepo;

    public BookingSlotService(RoomBookingSlotRepository slotRepo,
                              EventTimeSlotRepository eventTimeSlotRepo,
                              RoomRepository roomRepo) {
        this.slotRepo = slotRepo;
        this.eventTimeSlotRepo = eventTimeSlotRepo;
        this.roomRepo = roomRepo;
    }

    // ─── Slot Generation ──────────────────────────────────────────────

    /**
     * Generates booking slots for a newly created {@link RoomBookingRequest}.
     * <p>
     * For event-based bookings: reads {@link EventTimeSlot} rows and creates
     * one {@link RoomBookingSlot} per time slot (with overnight splitting).
     * <p>
     * For meeting bookings: creates slot(s) from meetingStart/meetingEnd
     * (with overnight splitting if needed).
     */
    @Transactional
    public List<RoomBookingSlot> generateSlots(RoomBookingRequest request) {
        List<RoomBookingSlot> slots = new ArrayList<>();

        if (request.getEvent() != null) {
            // Event-based: use EventTimeSlot rows
            List<EventTimeSlot> eventSlots = eventTimeSlotRepo
                    .findByEvent_IdOrderBySlotStartAsc(request.getEvent().getId());

            if (eventSlots.isEmpty()) {
                // Legacy fallback: use event's start/end directly
                Event ev = request.getEvent();
                slots.addAll(createSlotsForRange(request,
                        ev.getStartTime().toLocalDate(), ev.getStartTime().toLocalTime(),
                        ev.getEndTime().toLocalDate(), ev.getEndTime().toLocalTime()));
            } else {
                for (EventTimeSlot ets : eventSlots) {
                    slots.addAll(createSlotsForRange(request,
                            ets.getSlotStart().toLocalDate(), ets.getSlotStart().toLocalTime(),
                            ets.getSlotEnd().toLocalDate(), ets.getSlotEnd().toLocalTime()));
                }
            }
        } else if (request.getMeetingStart() != null && request.getMeetingEnd() != null) {
            // Meeting-based
            slots.addAll(createSlotsForRange(request,
                    request.getMeetingStart().toLocalDate(), request.getMeetingStart().toLocalTime(),
                    request.getMeetingEnd().toLocalDate(), request.getMeetingEnd().toLocalTime()));
        }

        if (!slots.isEmpty()) {
            slotRepo.saveAll(slots);
        }
        return slots;
    }

    /**
     * Creates slot(s) for a date-time range, splitting overnight ranges at midnight.
     * <p>
     * Example: April 15 15:00 → April 16 05:00 becomes:
     * <ul>
     *   <li>Slot 1: April 15, 15:00 → 23:59</li>
     *   <li>Slot 2: April 16, 00:00 → 05:00</li>
     * </ul>
     */
    private List<RoomBookingSlot> createSlotsForRange(RoomBookingRequest request,
                                                       LocalDate startDate, LocalTime startTime,
                                                       LocalDate endDate, LocalTime endTime) {
        List<RoomBookingSlot> result = new ArrayList<>();

        if (startDate.equals(endDate)) {
            // Same day — simple single slot
            result.add(new RoomBookingSlot(request, startDate, startTime, endTime));
        } else {
            // Multi-day or overnight: create slot per day
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                LocalTime dayStart = current.equals(startDate) ? startTime : LocalTime.of(0, 0);
                LocalTime dayEnd = current.equals(endDate) ? endTime : LocalTime.of(23, 59);
                result.add(new RoomBookingSlot(request, current, dayStart, dayEnd));
                current = current.plusDays(1);
            }
        }

        return result;
    }

    // ─── Per-Slot Availability ────────────────────────────────────────

    /**
     * Checks if a room is available for a specific slot (date + time range).
     * Considers both:
     * <ul>
     *   <li>Existing allocated slots in room_booking_slots</li>
     *   <li>Fixed timetable conflicts (via ScheduleService)</li>
     * </ul>
     */
    public boolean isSlotAvailable(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (roomId == null || date == null) return false;
        List<RoomBookingSlot> conflicts = slotRepo.findConflictingSlots(roomId, date, startTime, endTime);
        return conflicts.isEmpty();
    }

    /**
     * Checks availability of a room across ALL slots of a request.
     * Returns true only if the room is available for EVERY slot.
     */
    public boolean isRoomAvailableForAllSlots(Long roomId, List<RoomBookingSlot> slots) {
        for (RoomBookingSlot slot : slots) {
            if (!isSlotAvailable(roomId, slot.getSlotDate(), slot.getStartTime(), slot.getEndTime())) {
                return false;
            }
        }
        return true;
    }

    // ─── Per-Day Conflict Detection ──────────────────────────────────

    /**
     * Returns per-day conflict information for given room preferences.
     * <p>
     * Response structure per room:
     * <pre>
     * [
     *   { "date": "2026-04-15", "startTime": "10:00", "endTime": "14:00", "issues": ["Timetable conflict: ..."] },
     *   { "date": "2026-04-16", "startTime": "10:00", "endTime": "14:00", "issues": [] }
     * ]
     * </pre>
     */
    public Map<String, List<Map<String, Object>>> getPerDayConflicts(
            List<Long> roomIds, List<RoomBookingSlot> slots) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        for (Long roomId : roomIds) {
            if (roomId == null) continue;
            List<Map<String, Object>> dayConflicts = new ArrayList<>();

            for (RoomBookingSlot slot : slots) {
                Map<String, Object> dayInfo = new LinkedHashMap<>();
                dayInfo.put("date", slot.getSlotDate().toString());
                dayInfo.put("startTime", slot.getStartTime().toString());
                dayInfo.put("endTime", slot.getEndTime().toString());

                List<String> issues = new ArrayList<>();

                // Check slot-based booking conflicts
                List<RoomBookingSlot> conflicting = slotRepo.findConflictingSlots(
                        roomId, slot.getSlotDate(), slot.getStartTime(), slot.getEndTime());
                for (RoomBookingSlot cs : conflicting) {
                    RoomBookingRequest req = cs.getRequest();
                    String title = req.getEvent() != null ? req.getEvent().getTitle() : req.getMeetingPurpose();
                    issues.add("Booking conflict: " + (title != null ? title : "ID " + req.getId()));
                }

                dayInfo.put("issues", issues);
                dayConflicts.add(dayInfo);
            }

            result.put(roomId.toString(), dayConflicts);
        }

        return result;
    }

    // ─── Allocation ──────────────────────────────────────────────────

    /**
     * Bulk allocation: assigns the same room to ALL slots of a request.
     * Used when admin selects one room that is available for every day.
     */
    @Transactional
    public void allocateRoomToAllSlots(RoomBookingRequest request, Room room) {
        List<RoomBookingSlot> slots = slotRepo.findByRequestIdOrderBySlotDateAsc(request.getId());
        for (RoomBookingSlot slot : slots) {
            slot.setRoom(room);
            slot.setStatus("APPROVED");
        }
        slotRepo.saveAll(slots);
        // Also set the legacy allocatedRoom field for backward compat
        request.setAllocatedRoom(room);
    }

    /**
     * Per-slot allocation: assigns potentially different rooms to each slot.
     * Map key = slot ID, value = room ID.
     */
    @Transactional
    public void allocateRoomsPerSlot(RoomBookingRequest request, Map<Long, Long> slotRoomMap) {
        List<RoomBookingSlot> slots = slotRepo.findByRequestIdOrderBySlotDateAsc(request.getId());
        Room firstRoom = null;
        for (RoomBookingSlot slot : slots) {
            Long roomId = slotRoomMap.get(slot.getId());
            if (roomId != null) {
                Room room = roomRepo.findById(roomId).orElse(null);
                if (room != null) {
                    slot.setRoom(room);
                    slot.setStatus("APPROVED");
                    if (firstRoom == null) firstRoom = room;
                }
            }
        }
        slotRepo.saveAll(slots);
        // Legacy compat: set allocatedRoom to first slot's room
        if (firstRoom != null) {
            request.setAllocatedRoom(firstRoom);
        }
    }

    /**
     * Updates all slot statuses for a request (used by confirm scheduler).
     */
    @Transactional
    public void updateSlotStatuses(Long requestId, String newStatus) {
        List<RoomBookingSlot> slots = slotRepo.findByRequestIdOrderBySlotDateAsc(requestId);
        for (RoomBookingSlot slot : slots) {
            slot.setStatus(newStatus);
        }
        slotRepo.saveAll(slots);
    }

    /**
     * Returns the slots for a given request.
     */
    public List<RoomBookingSlot> getSlotsForRequest(Long requestId) {
        return slotRepo.findByRequestIdOrderBySlotDateAsc(requestId);
    }
}
