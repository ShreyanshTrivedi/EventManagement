package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.service.RoomAvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/faculty/bookings")
@PreAuthorize("hasRole('FACULTY')")
public class FacultyRoomBookingController {

    private final RoomBookingRequestRepository requestRepo;
    private final EventRepository eventRepo;
    private final RoomRepository roomRepo;
    private final RoomAvailabilityService availabilityService;

    public FacultyRoomBookingController(RoomBookingRequestRepository requestRepo,
                                        EventRepository eventRepo,
                                        RoomRepository roomRepo,
                                        RoomAvailabilityService availabilityService) {
        this.requestRepo = requestRepo;
        this.eventRepo = eventRepo;
        this.roomRepo = roomRepo;
        this.availabilityService = availabilityService;
    }

    public static class DirectBookBody {
        public Long eventId; // optional
        public Long roomId; // required
        /** Required: must match the building that contains {@code roomId}. */
        public Long buildingId;
        public LocalDateTime start; // required if eventId is null
        public LocalDateTime end;   // required if eventId is null
        public String purpose; // optional, used if eventId is null
    }

    private static boolean roomBelongsToBuilding(Room room, Long buildingId) {
        if (room == null || buildingId == null) return false;
        return room.getFloor() != null
                && room.getFloor().getBuilding() != null
                && buildingId.equals(room.getFloor().getBuilding().getId());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> directBook(@Valid @RequestBody DirectBookBody body,
                                        @AuthenticationPrincipal UserDetails principal) {
        if (body == null || body.roomId == null) {
            return ResponseEntity.badRequest().body("roomId is required");
        }
        if (body.buildingId == null) {
            return ResponseEntity.badRequest().body("buildingId is required");
        }
        Room room = roomRepo.findById(body.roomId).orElse(null);
        if (room == null) return ResponseEntity.badRequest().body("Room not found");
        if (!roomBelongsToBuilding(room, body.buildingId)) {
            return ResponseEntity.badRequest().body("Room does not belong to the selected building");
        }

        LocalDateTime start;
        LocalDateTime end;
        Event event = null;
        if (body.eventId != null) {
            event = eventRepo.findById(body.eventId).orElse(null);
            if (event == null) return ResponseEntity.badRequest().body("Event not found");
            start = event.getStartTime();
            end = event.getEndTime();
        } else {
            if (body.start == null || body.end == null || !body.end.isAfter(body.start)) {
                return ResponseEntity.badRequest().body("Valid start and end required");
            }
            start = body.start;
            end = body.end;
        }

        // Enforce server-side cutoff for direct bookings: start must be in the future
        if (event == null && !start.isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("start must be in the future");
        }

        boolean available = availabilityService.isRoomAvailable(room.getId(), start, end);
        if (!available) {
            return ResponseEntity.status(409).body("Room not available in the requested window");
        }

        RoomBookingRequest req = new RoomBookingRequest();
        req.setEvent(event);
        if (event == null) {
            req.setMeetingStart(start);
            req.setMeetingEnd(end);
            req.setMeetingPurpose(body.purpose);
        }
        req.setAllocatedRoom(room);
        req.setStatus(RoomBookingStatus.APPROVED);
        req.setApprovedAt(LocalDateTime.now());
        req.setApprovedByUsername(principal.getUsername());
        req.setRequestedByUsername(principal.getUsername());
        RoomBookingRequest saved = requestRepo.save(req);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name(), "allocatedRoom", room.getName()));
    }
}
