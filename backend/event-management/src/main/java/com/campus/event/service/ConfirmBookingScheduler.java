package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RoomBookingRequestRepository;
import com.campus.event.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ConfirmBookingScheduler {
    private static final Logger log = LoggerFactory.getLogger(ConfirmBookingScheduler.class);

    private final RoomBookingRequestRepository requestRepo;
    private final EventRepository eventRepo;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ConfirmBookingScheduler(RoomBookingRequestRepository requestRepo, EventRepository eventRepo,
                                   UserRepository userRepository, NotificationService notificationService) {
        this.requestRepo = requestRepo;
        this.eventRepo = eventRepo;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    // Run hourly at minute 0
    @Scheduled(cron = "0 0 * * * *")
    public void confirmApprovedBookings() {
        LocalDateTime cutoff = LocalDateTime.now().plusDays(2);
        List<RoomBookingRequest> toConfirm = requestRepo.findApprovedToConfirm(cutoff);
        for (RoomBookingRequest r : toConfirm) {
            try {
                if (r.getAllocatedRoom() == null) continue;
                Event evt = r.getEvent();
                if (evt == null) continue;
                // Update event location from allocated room
                if (evt.getLocation() == null || "TBD".equalsIgnoreCase(evt.getLocation())) {
                    evt.setLocation(r.getAllocatedRoom().getName());
                    eventRepo.save(evt);
                }
                r.setStatus(RoomBookingStatus.CONFIRMED);
                r.setConfirmedAt(LocalDateTime.now());
                requestRepo.save(r);
                log.info("Confirmed room for event {} (request {})", evt.getId(), r.getId());
                if (r.getRequestedByUsername() != null) {
                    userRepository.findByUsername(r.getRequestedByUsername()).ifPresent(u -> {
                        String subj = "Room booking confirmed";
                        String msg = "Your room booking request (ID " + r.getId() + ") is now CONFIRMED for room '" + r.getAllocatedRoom().getName() + "'.";
                        notificationService.notifyAllChannels(u, subj, msg);
                    });
                }
            } catch (Exception e) {
                log.warn("Failed to confirm booking request {}: {}", r.getId(), e.getMessage());
            }
        }
    }
}
