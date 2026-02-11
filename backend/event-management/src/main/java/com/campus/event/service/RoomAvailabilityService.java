package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.RoomBookingRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomAvailabilityService {
    private final RoomBookingRequestRepository requestRepo;

    public RoomAvailabilityService(RoomBookingRequestRepository requestRepo) {
        this.requestRepo = requestRepo;
    }

    public boolean isRoomAvailable(Long roomId, LocalDateTime start, LocalDateTime end) {
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return existing.stream()
                .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(roomId))
                .noneMatch(b -> overlaps(windowStart(b), windowEnd(b), start, end));
    }

    public Map<Long, Boolean> availabilityForRooms(List<Long> roomIds, LocalDateTime start, LocalDateTime end) {
        List<RoomBookingRequest> existing = requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED));
        return roomIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> existing.stream()
                        .filter(b -> b.getAllocatedRoom() != null && b.getAllocatedRoom().getId().equals(id))
                        .noneMatch(b -> overlaps(windowStart(b), windowEnd(b), start, end))
        ));
    }

    private static boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        if (aStart == null || aEnd == null || bStart == null || bEnd == null) return false;
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private static LocalDateTime windowStart(RoomBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getStartTime() != null) return e.getStartTime();
        return r.getMeetingStart();
        
    }

    private static LocalDateTime windowEnd(RoomBookingRequest r) {
        Event e = r.getEvent();
        if (e != null && e.getEndTime() != null) return e.getEndTime();
        return r.getMeetingEnd();
    }
}
