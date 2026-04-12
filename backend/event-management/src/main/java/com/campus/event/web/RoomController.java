package com.campus.event.web;

import com.campus.event.domain.Resource;
import com.campus.event.repository.ResourceRepository;
import com.campus.event.service.RoomAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final ResourceRepository resourceRepository;
    private final RoomAvailabilityService availabilityService;

    public RoomController(ResourceRepository resourceRepository, RoomAvailabilityService availabilityService) {
        this.resourceRepository = resourceRepository;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRooms() {
        return resourceRepository.findAll().stream()
            .filter(r -> r.getFloor() != null && r.getFloor().getBuilding() != null && r.getResourceType() != com.campus.event.domain.ResourceType.OPEN_SPACE)
            .map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("resourceId", r.getId());
            m.put("roomId", r.getRoomRefId() != null ? r.getRoomRefId() : r.getId());
            m.put("name", r.getName());
            m.put("roomNumber", "");
            m.put("type", r.getResourceType() != null ? r.getResourceType().name() : "");
            m.put("capacity", r.getCapacity());
            m.put("amenities", r.getAmenities());
            m.put("buildingId", r.getFloor().getBuilding().getId());
            m.put("buildingName", r.getFloor().getBuilding().getName());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> availability(@RequestParam LocalDateTime start,
                                                                  @RequestParam LocalDateTime end) {
        List<Resource> rooms = resourceRepository.findAll();
        List<Map<String, Object>> body = rooms.stream().filter(r -> r.getResourceType() != com.campus.event.domain.ResourceType.OPEN_SPACE).map(r -> {
            boolean available = availabilityService.isResourceAvailable(r.getId(), start, end);
            Map<String, Object> m = new HashMap<>();
            m.put("resourceId", r.getId());
            m.put("roomId", r.getRoomRefId() != null ? r.getRoomRefId() : r.getId());
            m.put("name", r.getName());
            m.put("available", available);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{roomId}/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    public ResponseEntity<Map<String, Object>> roomAvailability(@PathVariable Long roomId,
                                                                @RequestParam LocalDateTime start,
                                                                @RequestParam LocalDateTime end) {
        boolean available = availabilityService.isResourceAvailable(roomId, start, end);
        Resource r = resourceRepository.findById(roomId).orElse(null);
        long legacyId = r != null && r.getRoomRefId() != null ? r.getRoomRefId() : roomId;
        Map<String, Object> body = new HashMap<>();
        body.put("resourceId", roomId);
        body.put("roomId", legacyId);
        body.put("available", available);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/status-now")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> statusNow() {
        LocalDateTime now = LocalDateTime.now();
        List<Resource> rooms = resourceRepository.findAll();
        List<Map<String, Object>> body = rooms.stream().filter(r -> r.getResourceType() != com.campus.event.domain.ResourceType.OPEN_SPACE).map(r -> {
            boolean available = availabilityService.isResourceAvailable(r.getId(), now.minusMinutes(1), now.plusMinutes(1));
            Map<String, Object> m = new HashMap<>();
            m.put("resourceId", r.getId());
            m.put("roomId", r.getRoomRefId() != null ? r.getRoomRefId() : r.getId());
            m.put("name", r.getName());
            m.put("status", available ? "EMPTY" : "OCCUPIED");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }
}
