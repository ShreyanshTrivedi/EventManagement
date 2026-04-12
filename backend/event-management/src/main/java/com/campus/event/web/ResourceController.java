package com.campus.event.web;

import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceType;
import com.campus.event.repository.ResourceRepository;
import com.campus.event.service.RoomAvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for the unified Resource model.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/resources}                       – list all active resources (filterable by buildingId, type)</li>
 *   <li>{@code GET /api/resources/availability}          – availability status for a time window</li>
 *   <li>{@code GET /api/resources/suggest}               – ranked smart suggestion</li>
 *   <li>{@code GET /api/resources/{id}/waitlist-position}– caller's waitlist position for a resource-based event</li>
 * </ul>
 *
 * <p>These endpoints are non-breaking additions; existing {@code /api/rooms/**} endpoints are untouched.
 */
@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceRepository resourceRepository;
    private final RoomAvailabilityService availabilityService;

    public ResourceController(ResourceRepository resourceRepository,
                              RoomAvailabilityService availabilityService) {
        this.resourceRepository = resourceRepository;
        this.availabilityService = availabilityService;
    }

    // =========================================================================
    // GET /api/resources — list all active resources
    // Params: ?buildingId=&type=
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('GENERAL_USER','CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listResources(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String type) {

        List<Resource> resources;

        if (buildingId != null && type != null) {
            ResourceType rt = parseType(type);
            resources = rt == null
                    ? resourceRepository.findByBuilding_IdAndIsActiveTrueOrderByNameAsc(buildingId)
                    : resourceRepository.findByBuilding_IdAndResourceTypeAndIsActiveTrueOrderByNameAsc(buildingId, rt);
        } else if (buildingId != null) {
            resources = resourceRepository.findByBuilding_IdAndIsActiveTrueOrderByNameAsc(buildingId);
        } else if (type != null) {
            ResourceType rt = parseType(type);
            resources = rt == null
                    ? resourceRepository.findByIsActiveTrueOrderByNameAsc()
                    : resourceRepository.findByResourceTypeAndIsActiveTrueOrderByNameAsc(rt);
        } else {
            resources = resourceRepository.findByIsActiveTrueOrderByNameAsc();
        }

        return ResponseEntity.ok(resources.stream().map(this::toMap).collect(Collectors.toList()));
    }

    // =========================================================================
    // GET /api/resources/availability — availability for a time window
    // Params: ?start=&end=&buildingId=&type=
    // =========================================================================

    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> availability(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String type) {

        List<Resource> resources = filterResources(buildingId, type);

        List<Map<String, Object>> body = resources.stream().map(r -> {
            boolean avail = availabilityService.isResourceAvailable(r.getId(), start, end);
            Map<String, Object> m = toMap(r);
            m.put("available", avail);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(body);
    }

    // =========================================================================
    // GET /api/resources/suggest — smart ranked suggestion
    //
    // Scoring (100 max):
    //   Room too small (capacity < attendees)   → excluded entirely
    //   Room too large (capacity > attendees*3) → -20  (wasteful)
    //   Different building from requested       → -30
    //   Unavailable                             → score = 0, excluded
    //   Same building                           → bonus  0 (base)
    //   Best fit (capacity closest above need)  → stays near 100
    //
    // Returns up to 5 highest-scored results.
    // =========================================================================

    @GetMapping("/suggest")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> suggest(
            @RequestParam int attendees,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String type) {

        if (attendees <= 0 || start == null || end == null || !start.isBefore(end)) {
            return ResponseEntity.badRequest().body("attendees must be > 0 and start must be before end");
        }

        List<Resource> candidates = filterResources(buildingId, type);

        List<Map<String, Object>> ranked = candidates.stream()
                // Exclude resources too small to fit attendees
                .filter(r -> r.getCapacity() == null || r.getCapacity() >= attendees)
                // Exclude unavailable resources
                .filter(r -> availabilityService.isResourceAvailable(r.getId(), start, end))
                .map(r -> {
                    int score = 100;
                    List<String> reasons = new ArrayList<>();

                    // Capacity scoring
                    if (r.getCapacity() != null) {
                        if (r.getCapacity() > attendees * 3) {
                            score -= 20;
                            reasons.add("Oversized (capacity " + r.getCapacity() + " for " + attendees + " attendees)");
                        } else {
                            reasons.add("Good capacity fit (" + r.getCapacity() + " seats)");
                        }
                    } else {
                        reasons.add("Capacity unknown");
                    }

                    // Building preference
                    if (buildingId != null) {
                        Long resBuildingId = r.getBuilding() != null ? r.getBuilding().getId() : null;
                        if (!buildingId.equals(resBuildingId)) {
                            score -= 30;
                            reasons.add("Different building");
                        } else {
                            reasons.add("Correct building");
                        }
                    }

                    // Always available (already filtered above)
                    reasons.add("Available");

                    Map<String, Object> m = toMap(r);
                    m.put("score", score);
                    m.put("reasons", reasons);
                    return m;
                })
                .filter(m -> (int) m.get("score") > 0)
                .sorted(Comparator.comparingInt(m -> -((int) ((Map<?, ?>) m).get("score"))))
                .limit(5)
                .collect(Collectors.toList());

        if (ranked.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(ranked);
    }

    @GetMapping("/{resourceId}/availability")
    @PreAuthorize("hasAnyRole('CLUB_ASSOCIATE','FACULTY','ADMIN','CENTRAL_ADMIN','BUILDING_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> resourceAvailability(@PathVariable Long resourceId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {
        boolean available = availabilityService.isResourceAvailable(resourceId, start, end);
        Resource r = resourceRepository.findById(resourceId).orElse(null);
        long legacyRoomId = r != null && r.getRoomRefId() != null ? r.getRoomRefId() : resourceId;
        Map<String, Object> m = new HashMap<>();
        m.put("resourceId", resourceId);
        m.put("roomId", legacyRoomId);
        m.put("available", available);
        return ResponseEntity.ok(m);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Resource> filterResources(Long buildingId, String type) {
        if (buildingId != null && type != null) {
            ResourceType rt = parseType(type);
            return rt == null
                    ? resourceRepository.findByBuilding_IdAndIsActiveTrueOrderByNameAsc(buildingId)
                    : resourceRepository.findByBuilding_IdAndResourceTypeAndIsActiveTrueOrderByNameAsc(buildingId, rt);
        } else if (buildingId != null) {
            return resourceRepository.findByBuilding_IdAndIsActiveTrueOrderByNameAsc(buildingId);
        } else if (type != null) {
            ResourceType rt = parseType(type);
            return rt == null
                    ? resourceRepository.findByIsActiveTrueOrderByNameAsc()
                    : resourceRepository.findByResourceTypeAndIsActiveTrueOrderByNameAsc(rt);
        }
        return resourceRepository.findByIsActiveTrueOrderByNameAsc();
    }

    /** Maps a Resource to a JSON-friendly Map. */
    private Map<String, Object> toMap(Resource r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("resourceId", r.getId());
        m.put("roomId", r.getRoomRefId() != null ? r.getRoomRefId() : r.getId());
        m.put("name", r.getName());
        m.put("resourceType", r.getResourceType() != null ? r.getResourceType().name() : null);
        m.put("capacity", r.getCapacity());
        m.put("buildingId", r.getBuilding() != null ? r.getBuilding().getId() : null);
        m.put("buildingName", r.getBuilding() != null ? r.getBuilding().getName() : null);
        m.put("floorId", r.getFloor() != null ? r.getFloor().getId() : null);
        m.put("amenities", r.getAmenities());
        m.put("description", r.getDescription());
        m.put("isActive", r.isActive());
        m.put("roomRefId", r.getRoomRefId());
        return m;
    }

    /** Parses a type string to enum; returns null on unknown value (caller decides fallback). */
    private ResourceType parseType(String type) {
        try {
            return ResourceType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
