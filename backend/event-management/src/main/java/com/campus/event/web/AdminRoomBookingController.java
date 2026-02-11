package com.campus.event.web;

import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.RoomRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.service.NotificationService;
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

@RestController
@RequestMapping("/api/admin/room-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomBookingController {

    private final RoomBookingRequestRepository requestRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AdminRoomBookingController(RoomBookingRequestRepository requestRepo, RoomRepository roomRepo,
                                      UserRepository userRepository, NotificationService notificationService) {
        this.requestRepo = requestRepo;
        this.roomRepo = roomRepo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(@RequestParam(value = "status", required = false) String status) {
        List<RoomBookingRequest> list = status == null ?
                requestRepo.findAll() :
                requestRepo.findByStatus(RoomBookingStatus.valueOf(status));
        return list.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            // Support both event-based bookings and standalone meeting bookings
            if (r.getEvent() != null) {
                m.put("eventId", r.getEvent().getId());
                m.put("eventTitle", r.getEvent().getTitle());
                m.put("start", r.getEvent().getStartTime());
            } else {
                m.put("eventId", null);
                m.put("eventTitle", r.getMeetingPurpose());
                m.put("start", r.getMeetingStart());
            }
            m.put("status", r.getStatus().name());
            m.put("pref1", r.getPref1() != null ? r.getPref1().getName() : null);
            m.put("pref2", r.getPref2() != null ? r.getPref2().getName() : null);
            m.put("pref3", r.getPref3() != null ? r.getPref3().getName() : null);
            m.put("allocatedRoom", r.getAllocatedRoom() != null ? r.getAllocatedRoom().getName() : null);
            m.put("requestedBy", r.getRequestedByUsername());
            return m;
        }).collect(Collectors.toList());
    }

    public static class ApproveBody { public Long allocatedRoomId; }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestBody ApproveBody body,
                                     @AuthenticationPrincipal UserDetails principal) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
        if (body == null || body.allocatedRoomId == null) return ResponseEntity.badRequest().body("allocatedRoomId required");
        Room alloc = roomRepo.findById(body.allocatedRoomId).orElse(null);
        if (alloc == null) return ResponseEntity.badRequest().body("Room not found");

        // Prevent double-booking: check for overlapping approved/confirmed bookings for this room
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
        if (req.getRequestedByUsername() != null) {
            userRepository.findByUsername(req.getRequestedByUsername()).ifPresent(u -> {
                String subj = "Room request approved";
                String msg = "Your room request (ID " + req.getId() + ") has been approved for room '" + alloc.getName() + "'.";
                notificationService.notifyAllChannels(u, subj, msg);
            });
        }
        return ResponseEntity.ok("Approved");
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        RoomBookingRequest req = requestRepo.findById(id).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();
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
}
