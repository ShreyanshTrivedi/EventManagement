package com.campus.event.repository;

import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface RoomBookingRequestRepository extends JpaRepository<RoomBookingRequest, Long> {
    List<RoomBookingRequest> findByStatus(RoomBookingStatus status);

    @Query("select r from RoomBookingRequest r where r.status = 'APPROVED' and r.confirmedAt is null and ((r.event is not null and r.event.startTime <= ?1) or (r.event is null and r.meetingStart is not null and r.meetingStart <= ?1))")
    List<RoomBookingRequest> findApprovedToConfirm(LocalDateTime cutoff);

    List<RoomBookingRequest> findByRequestedByUsername(String username);

    List<RoomBookingRequest> findByStatusIn(Set<RoomBookingStatus> statuses);

    @Query("select r from RoomBookingRequest r where r.status = 'PENDING' and r.requestedAt <= ?1")
    List<RoomBookingRequest> findPendingOlderThan(LocalDateTime cutoff);
}
