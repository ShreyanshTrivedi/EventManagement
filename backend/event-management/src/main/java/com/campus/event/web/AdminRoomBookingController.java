package com.campus.event.web;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Event;
import com.campus.event.domain.Role;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.domain.User;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.NotificationService;
import com.campus.event.service.RoomApprovalRules;
import com.campus.event.service.ScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Room approvals: {@link Role#ADMIN} (full access) and scoped {@link Role#BUILDING_ADMIN}.
 * {@link Role#CENTRAL_ADMIN} is intentionally excluded — central admin handles roles and club assignment only.
 */
@RestController
@RequestMapping("/api/admin/room-requests")
@PreAuthorize("hasAnyRole('ADMIN', 'BUILDING_ADMIN')")
public class AdminRoomBookingController {

    private final RoomBookingRequestRepository requestRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EventRegistrationRepository registrationRepo;
    private final ScheduleService scheduleService;

    public AdminRoomBookingController(RoomBookingRequestRepository requestRepo, RoomRepository roomRepo,
                                      UserRepository userRepository, NotificationService notificationService,
                                      ScheduleService scheduleService,
                                      EventRegistrationRepository registrationRepo) {
        this.requestRepo = requestRepo;
        this.roomRepo = roomRepo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.scheduleService = scheduleService;
        this.registrationRepo = registrationRepo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(@RequestParam(value = "status", required = false) String status,
                                          @AuthenticationPrincipal UserDetails principal) {
        List<RoomBookingRequest> list = status == null
                ? requestRepo.findAll()
                : requestRepo.findByStatusOrderByRequestedAtDesc(RoomBookingStatus.valueOf(status));

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        boolean isSuperAdmin = currentUser != null && currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser != null && currentUser.getRoles().contains(Role.BUILDING_ADMIN);

        return list.stream()
                .filter(r -> isSuperAdmin || (isBuildingAdmin && visibleToBuildingAdmin(r, currentUser)))
                .map(r -> toDto(r))
                .collect(Collectors.toList());
    }

    private static boolean visibleToBuildingAdmin(RoomBookingRequest r, User admin) {
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
        for (Room p : List.of(r.getPref1(), r.getPref2(), r.getPref3())) {
            if (p == null) {
                continue;
            }
            any = true;
            if (p.getFloor() == null || p.getFloor().getBuilding() == null
                    || !bid.equals(p.getFloor().getBuilding().getId())) {
                return false;
            }
            if (RoomApprovalRules.scopeForRoom(p) != scope) {
                return false;
            }
        }
        if (!any) {
            return false;
        }
        if (r.getEvent() == null) {
            Room ref = Stream.of(r.getPref1(), r.getPref2(), r.getPref3()).filter(x -> x != null).findFirst().orElse(null);
            if (ref == null) {
                return false;
            }
            if (ref.getFloor() == null || ref.getFloor().getBuilding() == null
                    || !bid.equals(ref.getFloor().getBuilding().getId())) {
                return false;
            }
            return RoomApprovalRules.scopeForRoom(ref) == scope;
        }
        return true;
    }

    private Map<String, Object> toDto(RoomBookingRequest r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        if (r.getEvent() != null) {
            Event ev = r.getEvent();
            m.put("eventId", ev.getId());
            m.put("eventTitle", ev.getTitle());
            m.put("start", ev.getStartTime());
            m.put("registrationCount", registrationRepo.countByEvent_Id(ev.getId()));
            if (ev.getBuilding() != null) {
                m.put("buildingId", ev.getBuilding().getId());
                m.put("buildingName", ev.getBuilding().getName());
            }
        } else {
            m.put("eventId", null);
            m.put("eventTitle", r.getMeetingPurpose());
            m.put("start", r.getMeetingStart());
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
        m.put("pref1RoomType", r.getPref1() != null && r.getPref1().getType() != null ? r.getPref1().getType().name() : null);
        m.put("approvalScope", r.getPref1() != null ? RoomApprovalRules.scopeForRoom(r.getPref1()).name() : null);
        m.put("allocatedRoom", r.getAllocatedRoom() != null ? r.getAllocatedRoom().getName() : null);
        m.put("requestedBy", r.getRequestedByUsername());
        if (r.getSplitGroupId() != null) {
            m.put("splitGroupId", r.getSplitGroupId().toString());
            m.put("splitPart", Boolean.TRUE);
        } else {
            m.put("splitPart", Boolean.FALSE);
        }
        return m;
    }

    public static class ApproveBody {
        public Long allocatedRoomId;
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody ApproveBody body,
                                     @AuthenticationPrincipal UserDetails principal) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        if (body == null || body.allocatedRoomId == null) {
            return ResponseEntity.badRequest().body("allocatedRoomId required");
        }
        Room alloc = roomRepo.findById(body.allocatedRoomId).orElse(null);
        if (alloc == null) {
            return ResponseEntity.badRequest().body("Room not found");
        }

        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        boolean isSuperAdmin = currentUser != null && currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser != null && currentUser.getRoles().contains(Role.BUILDING_ADMIN);

        if (!isSuperAdmin) {
            if (!isBuildingAdmin || !visibleToBuildingAdmin(req, currentUser)) {
                return ResponseEntity.status(403).body("Not allowed to approve this request");
            }
            if (currentUser.getManagedBuildingId() == null || currentUser.getAdminScope() == null) {
                return ResponseEntity.status(403).body("Building admin must have managedBuildingId and adminScope");
            }
            if (alloc.getFloor() == null || alloc.getFloor().getBuilding() == null
                    || !currentUser.getManagedBuildingId().equals(alloc.getFloor().getBuilding().getId())) {
                return ResponseEntity.status(403).body("Allocated room must be in your building");
            }
            if (currentUser.getAdminScope() != RoomApprovalRules.scopeForRoom(alloc)) {
                return ResponseEntity.status(403).body("Allocated room type does not match your admin scope");
            }
        }

        LocalDateTime reqStart;
        LocalDateTime reqEnd;
        if (req.getEvent() != null && req.getEvent().getStartTime() != null && req.getEvent().getEndTime() != null) {
            reqStart = req.getEvent().getStartTime();
            reqEnd = req.getEvent().getEndTime();
        } else if (req.getMeetingStart() != null && req.getMeetingEnd() != null) {
            reqStart = req.getMeetingStart();
            reqEnd = req.getMeetingEnd();
        } else {
            reqStart = null;
            reqEnd = null;
        }

        if (reqStart != null && reqEnd != null) {
            List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
            boolean conflict = existing.stream()
                    .filter(b -> !b.getId().equals(req.getId()))
                    .filter(b -> b.getAllocatedRoom() != null && alloc.getId().equals(b.getAllocatedRoom().getId()))
                    .anyMatch(b -> {
                        LocalDateTime bStart;
                        LocalDateTime bEnd;
                        if (b.getEvent() != null && b.getEvent().getStartTime() != null && b.getEvent().getEndTime() != null) {
                            bStart = b.getEvent().getStartTime();
                            bEnd = b.getEvent().getEndTime();
                        } else if (b.getMeetingStart() != null && b.getMeetingEnd() != null) {
                            bStart = b.getMeetingStart();
                            bEnd = b.getMeetingEnd();
                        } else {
                            return false;
                        }
                        return reqStart.isBefore(bEnd) && reqEnd.isAfter(bStart);
                    });
            if (conflict) {
                return ResponseEntity.badRequest().body("Room is already booked in the requested time window");
            }
        }

        req.setAllocatedRoom(alloc);
        req.setStatus(RoomBookingStatus.APPROVED);
        req.setApprovedAt(LocalDateTime.now());
        req.setApprovedByUsername(principal.getUsername());
        requestRepo.save(req);
        rejectSplitSiblings(req);

        if (req.getRequestedByUsername() != null) {
            userRepository.findByUsername(req.getRequestedByUsername()).ifPresent(u -> {
                String subj = "Room request approved";
                String msg = "Your room request (ID " + req.getId() + ") has been approved for room '" + alloc.getName() + "'.";
                notificationService.notifyAllChannels(u, subj, msg);
            });
        }
        return ResponseEntity.ok("Approved");
    }

    private void rejectSplitSiblings(RoomBookingRequest approved) {
        if (approved.getSplitGroupId() == null) {
            return;
        }
        requestRepo.findBySplitGroupId(approved.getSplitGroupId()).stream()
                .filter(x -> !x.getId().equals(approved.getId()))
                .filter(x -> x.getStatus() == RoomBookingStatus.PENDING)
                .forEach(x -> {
                    x.setStatus(RoomBookingStatus.REJECTED);
                    requestRepo.save(x);
                });
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<?> reject(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        boolean isSuperAdmin = currentUser != null && currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser != null && currentUser.getRoles().contains(Role.BUILDING_ADMIN);
        if (!isSuperAdmin && (!isBuildingAdmin || !visibleToBuildingAdmin(req, currentUser))) {
            return ResponseEntity.status(403).body("Not allowed to reject this request");
        }

        if (req.getSplitGroupId() != null) {
            requestRepo.findBySplitGroupId(req.getSplitGroupId()).stream()
                    .filter(x -> x.getStatus() == RoomBookingStatus.PENDING)
                    .forEach(x -> {
                        x.setStatus(RoomBookingStatus.REJECTED);
                        requestRepo.save(x);
                    });
        } else {
            req.setStatus(RoomBookingStatus.REJECTED);
            requestRepo.save(req);
        }

        if (req.getRequestedByUsername() != null) {
            userRepository.findByUsername(req.getRequestedByUsername()).ifPresent(u -> {
                String subj = "Room request rejected";
                String msg = "Your room request (ID " + req.getId() + ") has been rejected.";
                notificationService.notifyAllChannels(u, subj, msg);
            });
        }
        return ResponseEntity.ok("Rejected");
    }

    @GetMapping("/{id}/conflicts")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getConflicts(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) {
            return ResponseEntity.notFound().build();
        }
        User currentUser = userRepository.findByUsername(principal.getUsername()).orElse(null);
        boolean isSuperAdmin = currentUser != null && currentUser.getRoles().contains(Role.ADMIN);
        boolean isBuildingAdmin = currentUser != null && currentUser.getRoles().contains(Role.BUILDING_ADMIN);
        if (!isSuperAdmin && (!isBuildingAdmin || !visibleToBuildingAdmin(req, currentUser))) {
            return ResponseEntity.status(403).build();
        }

        LocalDateTime start = req.getEvent() != null ? req.getEvent().getStartTime() : req.getMeetingStart();
        LocalDateTime end = req.getEvent() != null ? req.getEvent().getEndTime() : req.getMeetingEnd();

        if (start == null || end == null) {
            return ResponseEntity.ok(Map.of());
        }

        Map<String, List<String>> conflicts = scheduleService.validateEventRoomPreferences(
                req.getPref1() != null ? req.getPref1().getId() : null,
                req.getPref2() != null ? req.getPref2().getId() : null,
                req.getPref3() != null ? req.getPref3().getId() : null,
                start, end
        );
        return ResponseEntity.ok(conflicts);
    }
}
