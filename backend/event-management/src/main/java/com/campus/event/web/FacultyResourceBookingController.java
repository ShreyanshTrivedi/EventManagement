package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.ResourceType;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.ResourceBookingRequestRepository;
import com.campus.event.repository.ResourceRepository;
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
public class FacultyResourceBookingController {

    private final ResourceBookingRequestRepository requestRepo;
    private final EventRepository eventRepo;
    private final ResourceRepository resourceRepo;
    private final RoomAvailabilityService availabilityService;

    public FacultyResourceBookingController(ResourceBookingRequestRepository requestRepo,
                                        EventRepository eventRepo,
                                        ResourceRepository resourceRepo,
                                        RoomAvailabilityService availabilityService) {
        this.requestRepo = requestRepo;
        this.eventRepo = eventRepo;
        this.resourceRepo = resourceRepo;
        this.availabilityService = availabilityService;
    }

    public static class DirectBookBody {
        public Long eventId; // optional
        /** Preferred: unified resource id (ROOM or OPEN_SPACE for events). */
        public Long resourceId;
        /** Legacy alias for {@link #resourceId}. */
        public Long roomId;
        /** Required: must match the building that contains the resource. */
        public Long buildingId;
        public LocalDateTime start; // required if eventId is null
        public LocalDateTime end;   // required if eventId is null
        public String purpose; // optional, used if eventId is null
    }

    private static boolean roomBelongsToBuilding(Resource room, Long buildingId) {
        if (room == null || buildingId == null) return false;
        return room.getFloor() != null
                && room.getFloor().getBuilding() != null
                && buildingId.equals(room.getFloor().getBuilding().getId());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> directBook(@Valid @RequestBody DirectBookBody body,
                                        @AuthenticationPrincipal UserDetails principal) {
        Long targetId = body.resourceId != null ? body.resourceId : body.roomId;
        if (body == null || targetId == null) {
            return ResponseEntity.badRequest().body("resourceId or roomId is required");
        }
        if (body.buildingId == null) {
            return ResponseEntity.badRequest().body("buildingId is required");
        }
        Resource room = resourceRepo.findById(targetId).orElse(null);
        if (room == null) return ResponseEntity.badRequest().body("Resource not found");
        if (!roomBelongsToBuilding(room, body.buildingId)) {
            return ResponseEntity.badRequest().body("Resource does not belong to the selected building");
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
            if (room.getResourceType() == ResourceType.OPEN_SPACE) {
                return ResponseEntity.badRequest().body(
                        "OPEN_SPACE resources cannot be booked for standalone meetings; use an event or a ROOM.");
            }
        }

        // Enforce server-side cutoff for direct bookings: start must be in the future
        if (event == null && !start.isAfter(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("start must be in the future");
        }

        boolean available = availabilityService.isResourceAvailable(room.getId(), start, end);
        if (!available) {
            return ResponseEntity.status(409).body("Resource not available in the requested window");
        }

        ResourceBookingRequest req = new ResourceBookingRequest();
        req.setEvent(event);
        if (event == null) {
            req.setMeetingStart(start);
            req.setMeetingEnd(end);
            req.setMeetingPurpose(body.purpose);
        }
        req.setAllocatedResource(room);
        req.setStatus(RoomBookingStatus.APPROVED);
        req.setApprovedAt(LocalDateTime.now());
        req.setApprovedByUsername(principal.getUsername());
        req.setRequestedByUsername(principal.getUsername());
        ResourceBookingRequest saved = requestRepo.save(req);
        long legacyRoomId = room.getRoomRefId() != null ? room.getRoomRefId() : room.getId();
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "status", saved.getStatus().name(),
                "resourceId", room.getId(),
                "roomId", legacyRoomId,
                "allocatedRoom", room.getName()));
    }
}
