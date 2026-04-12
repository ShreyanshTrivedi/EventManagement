package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.domain.ResourceType;
import com.campus.event.repository.ResourceBookingRequestRepository;
import com.campus.event.repository.ResourceRepository;
import com.campus.event.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AutoAllocationScheduler {
    private static final Logger log = LoggerFactory.getLogger(AutoAllocationScheduler.class);

    private final ResourceBookingRequestRepository requestRepo;
    private final ResourceRepository resourceRepo;
    private final RoomAvailabilityService availabilityService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${app.allocations.auto.enabled:false}")
    private boolean enabled;

    @Value("${app.allocations.auto.timeoutMinutes:120}")
    private int timeoutMinutes;

    public AutoAllocationScheduler(ResourceBookingRequestRepository requestRepo, ResourceRepository resourceRepo, RoomAvailabilityService availabilityService,
                                   UserRepository userRepository, NotificationService notificationService) {
        this.requestRepo = requestRepo;
        this.resourceRepo = resourceRepo;
        this.availabilityService = availabilityService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Run every 10 minutes
    @Scheduled(cron = "0 */10 * * * *")
    public void autoAllocatePending() {
        if (!enabled) return;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ResourceBookingRequest> pending = requestRepo.findPendingOlderThan(cutoff);
        for (ResourceBookingRequest r : pending) {
            try {
                LocalDateTime start = windowStart(r);
                LocalDateTime end = windowEnd(r);
                if (start == null || end == null || !end.isAfter(start)) continue;
                Resource allocated = tryPreferences(r, start, end);
                if (allocated == null) allocated = tryAny(start, end);
                if (allocated != null) {
                    r.setAllocatedResource(allocated);
                    r.setStatus(RoomBookingStatus.APPROVED);
                    r.setApprovedAt(LocalDateTime.now());
                    r.setApprovedByUsername("AUTO");
                    requestRepo.save(r);
                    log.info("Auto-allocated request {} to room {}", r.getId(), allocated.getName());
                    if (r.getRequestedByUsername() != null) {
                        final String subj = "Room request auto-approved";
                        final String msg = "Your room request (ID " + r.getId() + ") has been auto-approved for room '" + allocated.getName() + "'.";
                        userRepository.findByUsername(r.getRequestedByUsername())
                                .ifPresent(u -> notificationService.notifyAllChannels(u, subj, msg));
                    }
                }
            } catch (Exception e) {
                log.warn("Auto-allocation failed for request {}: {}", r.getId(), e.getMessage());
            }
        }
    }

    private Resource tryPreferences(ResourceBookingRequest r, LocalDateTime start, LocalDateTime end) {
        Resource[] prefs = new Resource[]{r.getPref1(), r.getPref2(), r.getPref3()};
        for (Resource pref : prefs) {
            if (pref == null) continue;
            if (availabilityService.isResourceAvailable(pref.getId(), start, end)) return pref;
        }
        return null;
    }

    private Resource tryAny(LocalDateTime start, LocalDateTime end) {
        for (Resource resource : resourceRepo.findAll()) {
            if (resource.getResourceType() == ResourceType.ROOM && availabilityService.isResourceAvailable(resource.getId(), start, end)) return resource;
        }
        return null;
    }

    private static LocalDateTime windowStart(ResourceBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getStartTime() != null) return e.getStartTime();
        return r.getMeetingStart();
    }

    private static LocalDateTime windowEnd(ResourceBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getEndTime() != null) return e.getEndTime();
        return r.getMeetingEnd();
    }
}
