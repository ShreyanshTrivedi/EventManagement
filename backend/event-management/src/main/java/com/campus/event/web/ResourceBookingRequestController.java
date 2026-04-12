package com.campus.event.web;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.ResourceBookingRequestRepository;
import com.campus.event.repository.ResourceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.campus.event.service.BuildingTimetableService;
import com.campus.event.service.EventRoomBookingSplitService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room-requests")
public class ResourceBookingRequestController {

    private final ResourceBookingRequestRepository requestRepo;
    private final EventRepository eventRepo;
    private final ResourceRepository resourceRepo;
    private final com.campus.event.service.ScheduleService scheduleService;
    private final BuildingTimetableService buildingTimetableService;
    private final EventRoomBookingSplitService eventRoomBookingSplitService;
    private final EventTimeSlotRepository eventTimeSlotRepository;
    private final com.campus.event.service.RoomAvailabilityService availabilityService;

    public ResourceBookingRequestController(ResourceBookingRequestRepository requestRepo, EventRepository eventRepo, ResourceRepository resourceRepo,
                                            com.campus.event.service.ScheduleService scheduleService,
                                        BuildingTimetableService buildingTimetableService,
                                        EventRoomBookingSplitService eventRoomBookingSplitService,
                                        EventTimeSlotRepository eventTimeSlotRepository,
                                        com.campus.event.service.RoomAvailabilityService availabilityService) {
        this.requestRepo = requestRepo;
        this.eventRepo = eventRepo;
        this.resourceRepo = resourceRepo;
        this.scheduleService = scheduleService;
        this.buildingTimetableService = buildingTimetableService;
        this.eventRoomBookingSplitService = eventRoomBookingSplitService;
        this.eventTimeSlotRepository = eventTimeSlotRepository;
        this.availabilityService = availabilityService;
    }

    public static class CreateRequest {
        public Long eventId;
        public Long buildingId;
        public Long pref1RoomId;
        public Long pref1ResourceId;
        public Long pref2RoomId;
        public Long pref2ResourceId;
        public Long pref3RoomId;
        public Long pref3ResourceId;
        public LocalDateTime meetingStart;
        public LocalDateTime meetingEnd;
        public String meetingPurpose;
    }

    public static class SimpleBookingRequest {
        public Long roomId;
        public Long resourceId;
        public Long buildingId;
        public String date;
        public String purpose;
        public LocalDateTime meetingStart;
        public LocalDateTime meetingEnd;
    }

    private static boolean resourceBelongsToBuilding(Resource resource, Long buildingId) {
        if (resource == null || buildingId == null) return false;
        return resource.getFloor() != null
                && resource.getFloor().getBuilding() != null
                && buildingId.equals(resource.getFloor().getBuilding().getId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','CLUB_ASSOCIATE','FACULTY')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest req, @AuthenticationPrincipal UserDetails principal) {
        Long p1 = req.pref1ResourceId != null ? req.pref1ResourceId : req.pref1RoomId;
        Long p2 = req.pref2ResourceId != null ? req.pref2ResourceId : req.pref2RoomId;
        Long p3 = req.pref3ResourceId != null ? req.pref3ResourceId : req.pref3RoomId;

        if (p1 == null || p2 == null || p3 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Three resource preferences are required");
        }
        if (req.buildingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "buildingId is required");
        }

        boolean eventMode = req.eventId != null;
        boolean meetingMode = req.meetingStart != null && req.meetingEnd != null && req.meetingPurpose != null && !req.meetingPurpose.isBlank();

        if (eventMode == meetingMode) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specify either eventId or meetingStart/meetingEnd/meetingPurpose, but not both");
        }

        Event event = null;
        if (eventMode) {
            event = eventRepo.findById(req.eventId).orElse(null);
            if (event == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
            if (event.getBuilding() == null || !req.buildingId.equals(event.getBuilding().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "buildingId must match the event's building");
            }

            long days = Duration.between(LocalDateTime.now(), event.getStartTime()).toDays();
            if (days < 5) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum 5 days advance notice required for events");
        } else {
            // meeting mode
            if (!req.meetingEnd.isAfter(req.meetingStart)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meetingEnd must be after meetingStart");
            }
            if (!req.meetingStart.isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meetingStart must be in the future");
            }
        }

        Resource r1 = resourceRepo.findById(p1).orElse(null);
        Resource r2 = resourceRepo.findById(p2).orElse(null);
        Resource r3 = resourceRepo.findById(p3).orElse(null);
        if (r1 == null || r2 == null || r3 == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid resource preference(s)");
        if (!resourceBelongsToBuilding(r1, req.buildingId)
                || !resourceBelongsToBuilding(r2, req.buildingId)
                || !resourceBelongsToBuilding(r3, req.buildingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All selected resources must belong to the chosen building");
        }

        if (eventMode && event != null) {
            if (requestRepo.existsByEvent_IdAndStatus(event.getId(), RoomBookingStatus.PENDING)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A room booking request is already pending for this event.");
            }
            // For multi-day events, validate each time slot individually against building hours
            List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(event.getId());
            if (slots.isEmpty()) {
                // Legacy fallback: validate full range
                if (!buildingTimetableService.isBookingWithinBuildingHours(req.buildingId, event.getStartTime(), event.getEndTime())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event time is outside building operating hours");
                }
            } else {
                for (EventTimeSlot slot : slots) {
                    if (!buildingTimetableService.isBookingWithinBuildingHours(req.buildingId, slot.getSlotStart(), slot.getSlotEnd())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                            "Event time slot is outside building operating hours");
                    }
                }
            }
        } else {
            if (!buildingTimetableService.isBookingWithinBuildingHours(req.buildingId, req.meetingStart, req.meetingEnd)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meeting time is outside building operating hours");
            }
        }

        Map<String, List<String>> conflicts;
        if (eventMode && event != null) {
            // Use per-slot conflict detection for multi-day events
            List<EventTimeSlot> slots = eventTimeSlotRepository.findByEvent_IdOrderBySlotStartAsc(event.getId());
            if (slots.isEmpty()) {
                // Fallback for legacy events with no slots recorded
                conflicts = scheduleService.validateEventRoomPreferences(p1, p2, p3, event.getStartTime(), event.getEndTime());
            } else {
                conflicts = scheduleService.validateEventRoomPreferencesMultiSlot(p1, p2, p3, slots);
            }
        } else {
            conflicts = scheduleService.validateEventRoomPreferences(p1, p2, p3, req.meetingStart, req.meetingEnd);
        }

        if (eventMode) {
            List<ResourceBookingRequest> toSave = eventRoomBookingSplitService.buildEventRequests(event, r1, r2, r3, principal.getUsername());
            List<Long> ids = new ArrayList<>();
            ResourceBookingRequest first = null;
            for (ResourceBookingRequest rr : toSave) {
                ResourceBookingRequest saved = requestRepo.save(rr);
                ids.add(saved.getId());
                if (first == null) {
                    first = saved;
                }
            }
            Map<String, Object> body = new HashMap<>();
            body.put("id", first.getId());
            body.put("ids", ids);
            body.put("status", first.getStatus().name());
            body.put("conflicts", conflicts);
            if (first.getSplitGroupId() != null) {
                body.put("splitGroupId", first.getSplitGroupId().toString());
                body.put("splitApprovalNote",
                        "Mixed large-hall vs normal-room preferences: multiple pending requests were created. Approving one allocation automatically rejects the others in the same group.");
            }
            return ResponseEntity.ok(body);
        }

        ResourceBookingRequest rr = new ResourceBookingRequest();
        rr.setEvent(null);
        rr.setMeetingStart(req.meetingStart);
        rr.setMeetingEnd(req.meetingEnd);
        rr.setMeetingPurpose(req.meetingPurpose);
        rr.setPref1(r1);
        rr.setPref2(r2);
        rr.setPref3(r3);
        rr.setStatus(RoomBookingStatus.PENDING);
        rr.setRequestedByUsername(principal.getUsername());
        ResourceBookingRequest saved = requestRepo.save(rr);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "status", saved.getStatus().name(), "conflicts", conflicts));
    }

    @PostMapping("/meeting")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','FACULTY','CLUB_ASSOCIATE')")
    @Transactional
    public ResponseEntity<?> createSimpleMeeting(@Valid @RequestBody SimpleBookingRequest req, @AuthenticationPrincipal UserDetails principal) {
        Long resolvedResourceId = req.resourceId != null ? req.resourceId : req.roomId;

        if (resolvedResourceId == null || req.meetingStart == null || req.meetingEnd == null || req.purpose == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "resourceId (or roomId), meetingStart, meetingEnd, and purpose are required");
        }
        if (req.buildingId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "buildingId is required");
        }
        if (!req.meetingEnd.isAfter(req.meetingStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meetingEnd must be after meetingStart");
        }
        if (!req.meetingStart.isAfter(java.time.LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "meetingStart must be in the future");
        }

        Resource resource = resourceRepo.findByIdWithLock(resolvedResourceId).orElse(null);
        if (resource == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
        }
        if (!resourceBelongsToBuilding(resource, req.buildingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Resource does not belong to the selected building");
        }
        if (resource.getResourceType() == com.campus.event.domain.ResourceType.OPEN_SPACE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot book OPEN_SPACE directly for a meeting");
        }

        boolean available = availabilityService.isResourceAvailable(resource.getId(), req.meetingStart, req.meetingEnd);
        if (!available) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Resource is not available in the requested window");
        }

        ResourceBookingRequest bookingRequest = new ResourceBookingRequest();
        bookingRequest.setMeetingStart(req.meetingStart);
        bookingRequest.setMeetingEnd(req.meetingEnd);
        bookingRequest.setMeetingPurpose(req.purpose);
        bookingRequest.setPref1(resource);
        bookingRequest.setPref2(resource);
        bookingRequest.setPref3(resource);
        bookingRequest.setAllocatedResource(resource);
        bookingRequest.setStatus(RoomBookingStatus.APPROVED);
        bookingRequest.setApprovedAt(java.time.LocalDateTime.now());
        bookingRequest.setApprovedByUsername(principal.getUsername());
        bookingRequest.setRequestedByUsername(principal.getUsername());
        
        ResourceBookingRequest saved = requestRepo.save(bookingRequest);
        long legacyRoomId = resource.getRoomRefId() != null ? resource.getRoomRefId() : resource.getId();
        return ResponseEntity.ok(Map.of(
                "id", saved.getId(),
                "status", saved.getStatus().name(),
                "allocatedRoom", resource.getName(),
                "resourceId", resource.getId(),
                "roomId", legacyRoomId
        ));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','FACULTY','CLUB_ASSOCIATE')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(@AuthenticationPrincipal UserDetails principal) {
        return requestRepo.findByRequestedByUsernameOrderByRequestedAtDesc(principal.getUsername())
                .stream()
                .map(r -> {
                    java.util.HashMap<String, Object> m = new java.util.HashMap<>();
                    m.put("id", r.getId());
                    boolean hasEvent = r.getEvent() != null;
                    m.put("eventId", hasEvent ? r.getEvent().getId() : null);
                    m.put("eventTitle", hasEvent ? r.getEvent().getTitle() : r.getMeetingPurpose());
                    m.put("start", hasEvent ? r.getEvent().getStartTime() : r.getMeetingStart());
                    m.put("end", hasEvent ? r.getEvent().getEndTime() : r.getMeetingEnd());
                    m.put("status", r.getStatus().name());
                    m.put("allocatedRoom", r.getAllocatedResource() != null ? r.getAllocatedResource().getName() : null);
                    m.put("pref1", r.getPref1() != null ? r.getPref1().getName() : null);
                    m.put("pref2", r.getPref2() != null ? r.getPref2().getName() : null);
                    m.put("pref3", r.getPref3() != null ? r.getPref3().getName() : null);
                    if (r.getSplitGroupId() != null) {
                        m.put("splitGroupId", r.getSplitGroupId().toString());
                    }
                    if (r.getEvent() != null && r.getEvent().getBuilding() != null) {
                        m.put("buildingName", r.getEvent().getBuilding().getName());
                    }
                    if (r.getPref1() != null) {
                        m.put("approvalScope", com.campus.event.service.RoomApprovalRules.scopeForResource(r.getPref1()).name());
                    }
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','BUILDING_ADMIN','CENTRAL_ADMIN','FACULTY','CLUB_ASSOCIATE')")
    @Transactional
    public ResponseEntity<?> cancel(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ResourceBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
        if (!principal.getUsername().equals(req.getRequestedByUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to cancel this request");
        }

        List<ResourceBookingRequest> batch = req.getSplitGroupId() == null
                ? List.of(req)
                : requestRepo.findBySplitGroupId(req.getSplitGroupId()).stream()
                .filter(x -> principal.getUsername().equals(x.getRequestedByUsername()))
                .toList();

        for (ResourceBookingRequest r : batch) {
            if (r.getStatus() != RoomBookingStatus.PENDING) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending requests can be cancelled");
            }
        }

        LocalDateTime start;
        if (req.getEvent() != null && req.getEvent().getStartTime() != null) {
            start = req.getEvent().getStartTime();
        } else {
            start = req.getMeetingStart();
        }
        if (start != null && LocalDateTime.now().isAfter(start.minusDays(2))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot cancel within 2 days of the booking start");
        }

        for (ResourceBookingRequest r : batch) {
            r.setStatus(RoomBookingStatus.REJECTED);
            requestRepo.save(r);
        }
        return ResponseEntity.ok("Cancelled");
    }
}
