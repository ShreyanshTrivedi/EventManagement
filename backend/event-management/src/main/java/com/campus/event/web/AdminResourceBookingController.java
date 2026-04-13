package com.campus.event.web;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Event;
import com.campus.event.domain.EventStatus;
import com.campus.event.domain.EventTimeSlot;
import com.campus.event.domain.Role;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingSlot;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.EventTimeSlotRepository;
import com.campus.event.repository.ResourceBookingRequestRepository;
import com.campus.event.repository.ResourceRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.BookingSlotService;
import com.campus.event.service.NotificationService;
import com.campus.event.service.RoomApprovalRules;
import com.campus.event.service.ScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Room approvals: {@link Role#ADMIN} (full access) and scoped {@link Role#BUILDING_ADMIN}.
 * {@link Role#CENTRAL_ADMIN} is intentionally excluded — central admin handles roles and club assignment only.
 */
@RestController
@RequestMapping("/api/admin/room-requests")
@PreAuthorize("hasAnyRole('ADMIN', 'BUILDING_ADMIN')")
public class AdminResourceBookingController {

    /** Minutes before an admin claim expires and another admin may claim the request. */
    private static final int CLAIM_EXPIRY_MINUTES = 15;

    private final ResourceBookingRequestRepository requestRepo;
    private final ResourceRepository resourceRepo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EventRegistrationRepository registrationRepo;
    private final ScheduleService scheduleService;
    private final EventTimeSlotRepository eventTimeSlotRepository;
    private final BookingSlotService bookingSlotService;

    public AdminResourceBookingController(ResourceBookingRequestRepository requestRepo, ResourceRepository resourceRepo,
                                      UserRepository userRepository, NotificationService notificationService,
                                      ScheduleService scheduleService,
                                      EventRegistrationRepository registrationRepo,
                                      EventTimeSlotRepository eventTimeSlotRepository,
                                      BookingSlotService bookingSlotService) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.scheduleService = scheduleService;
        this.registrationRepo = registrationRepo;
        this.eventTimeSlotRepository = eventTimeSlotRepository;
        this.bookingSlotService = bookingSlotService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(@RequestParam(value = "status", required = false) String status,
                                          @AuthenticationPrincipal UserDetails principal) {
        List<ResourceBookingRequest> list = status == null
                ? requestRepo.findRecentBookings(org.springframework.data.domain.PageRequest.of(0, 200)).getContent()
                : requestRepo.findByStatusOrderByRequestedAtDesc(RoomBookingStatus.valueOf(status));

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (currentUser == null) {
            return java.util.Collections.emptyList();
        }

        boolean isSuperAdmin = currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser.getRoles().contains(Role.BUILDING_ADMIN);

        // IMPORTANT:
        // - BUILDING_ADMIN is always bounded by (managedBuildingId + adminScope)
        // - ADMIN behaves as "building admin" only if they have a managed building configured.
        //   Otherwise, ADMIN is treated as super admin and can see all requests.
        boolean adminIsBounded = currentUser.getManagedBuildingId() != null && currentUser.getAdminScope() != null;

        return list.stream()
                .filter(r -> {
                    if (isBuildingAdmin) {
                        return visibleToBuildingAdmin(r, currentUser);
                    }
                    if (isSuperAdmin) {
                        return !adminIsBounded || visibleToBuildingAdmin(r, currentUser);
                    }
                    return false;
                })
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private static boolean visibleToBuildingAdmin(ResourceBookingRequest r, User admin) {
        Long bid = admin.getManagedBuildingId();
        AdminScope scope = admin.getAdminScope();
        if (bid == null || scope == null) {
            return false;
        }
        if (r.getEvent() != null) {
            Event ev = r.getEvent();
            if (ev.getBuilding() == null || !bid.equals(ev.getBuilding().getId())) {
                return false;
            }
        }
        boolean any = false;
        for (Resource p : Arrays.asList(r.getPref1(), r.getPref2(), r.getPref3())) {
            if (p == null) {
                continue;
            }
            any = true;
            if (p.getFloor() == null || p.getFloor().getBuilding() == null
                    || !bid.equals(p.getFloor().getBuilding().getId())) {
                return false;
            }
            if (RoomApprovalRules.scopeForResource(p) != scope) {
                return false;
            }
        }
        if (!any) {
            return false;
        }
        if (r.getEvent() == null) {
            Resource ref = Stream.of(r.getPref1(), r.getPref2(), r.getPref3()).filter(x -> x != null).findFirst().orElse(null);
            if (ref == null) {
                return false;
            }
            if (ref.getFloor() == null || ref.getFloor().getBuilding() == null
                    || !bid.equals(ref.getFloor().getBuilding().getId())) {
                return false;
            }
            return RoomApprovalRules.scopeForResource(ref) == scope;
        }
        return true;
    }

    private Map<String, Object> toDto(ResourceBookingRequest r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        if (r.getEvent() != null) {
            Event ev = r.getEvent();
            m.put("eventId", ev.getId());
            m.put("eventTitle", ev.getTitle());
            m.put("start", ev.getStartTime());
            m.put("end", ev.getEndTime());
            m.put("registrationCount", registrationRepo.countByEvent_Id(ev.getId()));
            if (ev.getTimingModel() != null) {
                m.put("timingModel", ev.getTimingModel().name());
            }
            if (ev.getBuilding() != null) {
                m.put("buildingId", ev.getBuilding().getId());
                m.put("buildingName", ev.getBuilding().getName());
            }
        } else {
            m.put("eventId", null);
            m.put("eventTitle", r.getMeetingPurpose());
            m.put("start", r.getMeetingStart());
            m.put("end", r.getMeetingEnd());
            m.put("timingModel", "SINGLE_DAY");
            Room ref = r.getPref1();
            if (ref != null && ref.getFloor() != null && ref.getFloor().getBuilding() != null) {
                m.put("buildingId", ref.getFloor().getBuilding().getId());
                m.put("buildingName", ref.getFloor().getBuilding().getName());
            }
        }
        m.put("status", r.getStatus().name());
        m.put("pref1", r.getPref1() != null ? r.getPref1().getName() : null);
        m.put("pref2", r.getPref2() != null ? r.getPref2().getName() : null);
        m.put("pref3", r.getPref3() != null ? r.getPref3().getName() : null);
        m.put("pref1Id", r.getPref1() != null ? r.getPref1().getId() : null);
        m.put("pref2Id", r.getPref2() != null ? r.getPref2().getId() : null);
        m.put("pref3Id", r.getPref3() != null ? r.getPref3().getId() : null);
        m.put("pref1RoomType", r.getPref1() != null && r.getPref1().getResourceType() != null ? r.getPref1().getResourceType().name() : null);
        m.put("approvalScope", r.getPref1() != null ? RoomApprovalRules.scopeForResource(r.getPref1()).name() : null);
        if (r.getAllocatedResource() != null) {
            Resource ar = r.getAllocatedResource();
            m.put("allocatedResourceId", ar.getId());
            m.put("allocatedRoomId", ar.getRoomRefId() != null ? ar.getRoomRefId() : ar.getId());
            m.put("allocatedRoom", ar.getName());
        } else {
            m.put("allocatedResourceId", null);
            m.put("allocatedRoomId", null);
            m.put("allocatedRoom", null);
        }
        m.put("requestedBy", r.getRequestedByUsername());
        if (r.getClaimedBy() != null) {
            m.put("claimedBy", r.getClaimedBy().getUsername());
        } else {
            m.put("claimedBy", null);
        }
        m.put("claimedAt", r.getClaimedAt());
        if (r.getClaimedAt() != null) {
            m.put("claimExpiresAt", r.getClaimedAt().plusMinutes(CLAIM_EXPIRY_MINUTES));
        } else {
            m.put("claimExpiresAt", null);
        }
        if (r.getSplitGroupId() != null) {
            m.put("splitGroupId", r.getSplitGroupId().toString());
            m.put("splitPart", Boolean.TRUE);
        } else {
            m.put("splitPart", Boolean.FALSE);
        }

        // Booking slots — per-day breakdown with room allocation status
        List<RoomBookingSlot> bookingSlots = bookingSlotService.getSlotsForRequest(r.getId());
        m.put("slotCount", bookingSlots.size());
        m.put("bookingSlots", bookingSlots.stream().map(s -> {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.getId());
            sm.put("date", s.getSlotDate().toString());
            sm.put("startTime", s.getStartTime().toString());
            sm.put("endTime", s.getEndTime().toString());
            sm.put("roomId", s.getRoom() != null ? s.getRoom().getId() : null);
            sm.put("roomName", s.getRoom() != null ? s.getRoom().getName() : null);
            sm.put("status", s.getStatus());
            return sm;
        }).collect(Collectors.toList()));

        return m;
    }

    /**
     * Approval request body. Supports two modes:
     * <ul>
     *   <li><b>Bulk</b>: set {@code allocatedRoomId} — allocates same room to ALL slots
     *       (only if available for every slot)</li>
     *   <li><b>Per-slot</b>: set {@code slotAllocations} — map of slotId → roomId</li>
     * </ul>
     */
    public static class ApproveBody {
        public Long allocatedResourceId;
        public Long allocatedRoomId;
        public List<SlotAllocation> slotAllocations;
    }

    public static class SlotAllocation {
        public Long slotId;
        public Long roomId;
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody ApproveBody body,
                                     @AuthenticationPrincipal UserDetails principal) {
        ResourceBookingRequest req = requestRepo.findByIdForUpdate(id).orElse(null);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        Long allocId = body != null && body.allocatedResourceId != null
                ? body.allocatedResourceId
                : (body != null ? body.allocatedRoomId : null);
        if (body == null || allocId == null) {
            return ResponseEntity.badRequest().body("allocatedResourceId or allocatedRoomId required");
        }
        if (!callerOwnsActiveClaim(req, principal.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You must hold an active claim on this request to approve it");
        }
        // Force database-level pessimistic lock to serialize approvals for this room
        Resource alloc = resourceRepo.findByIdWithLock(allocId).orElse(null);
        if (alloc == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }
        if (req.getEvent() == null && alloc.getResourceType() == ResourceType.OPEN_SPACE) {
            return ResponseEntity.badRequest().body("Meetings can only be allocated to ROOM-type resources, not OPEN_SPACE");
        }

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(403).body("Not allowed to approve this request");
        }
        boolean isSuperAdmin = currentUser != null && currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser != null && currentUser.getRoles().contains(Role.BUILDING_ADMIN);
        boolean adminIsBounded = currentUser != null && currentUser.getManagedBuildingId() != null && currentUser.getAdminScope() != null;

        boolean bypassChecks = isSuperAdmin && !adminIsBounded;

        if (!bypassChecks) {
            if (!(isBuildingAdmin || isSuperAdmin) || !visibleToBuildingAdmin(req, currentUser)) {
                return ResponseEntity.status(403).body("Not allowed to approve this request");
            }
            if (currentUser.getManagedBuildingId() == null || currentUser.getAdminScope() == null) {
                return ResponseEntity.status(403).body("Admin must have managedBuildingId and adminScope when evaluating bounded access");
            }
            if (alloc.getFloor() == null || alloc.getFloor().getBuilding() == null
                    || !currentUser.getManagedBuildingId().equals(alloc.getFloor().getBuilding().getId())) {
                return ResponseEntity.status(403).body("Allocated room must be in your building");
            }
            if (currentUser.getAdminScope() != RoomApprovalRules.scopeForResource(alloc)) {
                return ResponseEntity.status(403).body("Allocated room type does not match your admin scope");
            }
        }

        // ── SLOT-BASED ALLOCATION ──
        List<RoomBookingSlot> bookingSlots = bookingSlotService.getSlotsForRequest(req.getId());

        boolean isPerSlot = body.slotAllocations != null && !body.slotAllocations.isEmpty();

        if (isPerSlot) {
            // Per-slot allocation: validate each slot's room
            Map<Long, Long> slotRoomMap = new LinkedHashMap<>();
            for (SlotAllocation sa : body.slotAllocations) {
                if (sa.slotId == null || sa.roomId == null) continue;
                Room slotRoom = roomRepo.findById(sa.roomId).orElse(null);
                if (slotRoom == null) {
                    return ResponseEntity.badRequest().body("Room not found for slot " + sa.slotId);
                }
                // Per-slot conflict check
                RoomBookingSlot slot = bookingSlots.stream()
                        .filter(s -> s.getId().equals(sa.slotId)).findFirst().orElse(null);
                if (slot == null) {
                    return ResponseEntity.badRequest().body("Slot not found: " + sa.slotId);
                }
                List<String> dayConflicts = scheduleService.getRoomConflictsForSlot(
                        sa.roomId, slot.getSlotDate(), slot.getStartTime(), slot.getEndTime());
                if (!dayConflicts.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            "Room conflict on " + slot.getSlotDate() + ": " + dayConflicts);
                }
                slotRoomMap.put(sa.slotId, sa.roomId);
            }
            bookingSlotService.allocateRoomsPerSlot(req, slotRoomMap);
        } else {
            // Bulk allocation: same room for ALL slots — must be available for every one
            for (RoomBookingSlot slot : bookingSlots) {
                if (!bookingSlotService.isSlotAvailable(alloc.getId(), slot.getSlotDate(),
                        slot.getStartTime(), slot.getEndTime())) {
                    return ResponseEntity.badRequest().body(
                            "Room '" + alloc.getName() + "' has a conflict on " + slot.getSlotDate()
                            + " (" + slot.getStartTime() + "–" + slot.getEndTime() + ")");
                }
                List<String> dayConflicts = scheduleService.getRoomConflictsForSlot(
                        alloc.getId(), slot.getSlotDate(), slot.getStartTime(), slot.getEndTime());
                if (!dayConflicts.isEmpty()) {
                    return ResponseEntity.badRequest().body(
                            "Timetable conflict on " + slot.getSlotDate() + ": " + dayConflicts);
                }
            }
            bookingSlotService.allocateRoomToAllSlots(req, alloc);
        }

        req.setStatus(RoomBookingStatus.APPROVED);
        req.setApprovedAt(LocalDateTime.now());
        req.setApprovedByUsername(principal.getUsername());
        requestRepo.save(req);
        rejectSplitSiblings(req);

        // ── State machine: PENDING → APPROVED ──────────────────────────────
        if (req.getEvent() != null) {
            Event ev = eventRepository.findById(req.getEvent().getId()).orElse(null);
            if (ev != null && ev.getStatus() == EventStatus.PENDING) {
                ev.setStatus(EventStatus.APPROVED);
                eventRepository.save(ev);

                // ── Notify all registered students that the event is live ──
                String liveSubj = "Event confirmed: " + ev.getTitle();
                String liveMsg  = "Great news! '" + ev.getTitle() + "' on "
                        + ev.getStartTime().toLocalDate()
                        + " has been confirmed and a room has been allocated: "
                        + alloc.getName() + ". See you there!";
                registrationRepo.findByEvent_Id(ev.getId()).forEach(reg -> {
                    if (reg.getUser() != null) {
                        notificationService.notifyAllChannels(reg.getUser(), liveSubj, liveMsg);
                    }
                });
            }
        }

        // ── Notify requester ────────────────────────────────────────────────
        if (req.getRequestedByUsername() != null) {
            String roomDesc = isPerSlot ? "per-day rooms" : ("room '" + alloc.getName() + "'");
            userRepository.findByUsername(req.getRequestedByUsername()).ifPresent(u -> {
                String subj = "Room request approved";
                String msg = "Your room request (ID " + req.getId() + ") has been approved for " + roomDesc + ".";
                notificationService.notifyAllChannels(u, subj, msg);
            });
        }
        return ResponseEntity.ok("Approved");
    }

    private void rejectSplitSiblings(ResourceBookingRequest approved) {
        if (approved.getSplitGroupId() != null) {
            requestRepo.rejectSplitSiblingsBulk(approved.getSplitGroupId(), approved.getId());
        }
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ResourceBookingRequest req = requestRepo.findByIdForUpdate(id).orElse(null);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(403).body("Not allowed to reject this request");
        }
        boolean isSuperAdmin = currentUser != null && currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser != null && currentUser.getRoles().contains(Role.BUILDING_ADMIN);
        boolean adminIsBounded = currentUser != null && currentUser.getManagedBuildingId() != null && currentUser.getAdminScope() != null;
        boolean bypassChecks = isSuperAdmin && !adminIsBounded;

        if (!bypassChecks) {
            if (!(isBuildingAdmin || isSuperAdmin) || !visibleToBuildingAdmin(req, currentUser)) {
                return ResponseEntity.status(403).body("Not allowed to reject this request");
            }
        }
        if (!callerOwnsActiveClaim(req, principal.getUsername())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You must hold an active claim on this request to reject it");
        }

        req.setStatus(RoomBookingStatus.REJECTED);
        requestRepo.save(req);

        if (req.getRequestedByUsername() != null) {
            userRepository.findByUsername(req.getRequestedByUsername()).ifPresent(u -> {
                String subj = "Room request rejected";
                String msg = "Your room request (ID " + req.getId() + ") has been rejected.";
                notificationService.notifyAllChannels(u, subj, msg);
            });
        }
        return ResponseEntity.ok("Rejected");
    }

    /**
     * Per-day conflict check using the slot-based model.
     * Returns per-room, per-day conflict structure.
     */
    @GetMapping("/{id}/conflicts")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getConflicts(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        ResourceBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(403).build();
        }
        boolean isSuperAdmin = currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser.getRoles().contains(Role.BUILDING_ADMIN);
        boolean adminIsBounded = currentUser.getManagedBuildingId() != null && currentUser.getAdminScope() != null;
        boolean bypassChecks = isSuperAdmin && !adminIsBounded;

        if (!bypassChecks) {
            if (!(isBuildingAdmin || isSuperAdmin) || !visibleToBuildingAdmin(req, currentUser)) {
                return ResponseEntity.status(403).build();
            }
        }

        // Get the booking slots for this request
        List<RoomBookingSlot> bookingSlots = bookingSlotService.getSlotsForRequest(req.getId());

        // Collect unique preference room IDs
        List<Long> prefRoomIds = Stream.of(
                req.getPref1() != null ? req.getPref1().getId() : null,
                req.getPref2() != null ? req.getPref2().getId() : null,
                req.getPref3() != null ? req.getPref3().getId() : null
        ).filter(x -> x != null).distinct().collect(Collectors.toList());

        if (bookingSlots.isEmpty() || prefRoomIds.isEmpty()) {
            return ResponseEntity.ok(Map.of());
        }

        // Build per-day, per-room conflict map using slot-based model
        Map<String, List<Map<String, Object>>> conflicts = bookingSlotService.getPerDayConflicts(prefRoomIds, bookingSlots);

        // Enrich with timetable conflicts from ScheduleService
        for (Long roomId : prefRoomIds) {
            List<Map<String, Object>> dayList = conflicts.get(roomId.toString());
            if (dayList == null) continue;
            for (Map<String, Object> dayInfo : dayList) {
                java.time.LocalDate date = java.time.LocalDate.parse((String) dayInfo.get("date"));
                java.time.LocalTime st = java.time.LocalTime.parse((String) dayInfo.get("startTime"));
                java.time.LocalTime et = java.time.LocalTime.parse((String) dayInfo.get("endTime"));
                List<String> timetableIssues = scheduleService.getRoomConflictsForSlot(roomId, date, st, et);
                @SuppressWarnings("unchecked")
                List<String> issues = (List<String>) dayInfo.get("issues");
                issues.addAll(timetableIssues);
            }
        }

        return ResponseEntity.ok(conflicts);
    }
}
