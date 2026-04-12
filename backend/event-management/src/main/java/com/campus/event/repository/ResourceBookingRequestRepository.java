package com.campus.event.repository;

import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ResourceBookingRequestRepository extends JpaRepository<ResourceBookingRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResourceBookingRequest r WHERE r.id = :id")
    Optional<ResourceBookingRequest> findByIdForUpdate(@Param("id") Long id);

    List<ResourceBookingRequest> findByRequestedByUsernameOrderByRequestedAtDesc(String username);
    
    List<ResourceBookingRequest> findByStatusOrderByRequestedAtDesc(RoomBookingStatus status);
    
    List<ResourceBookingRequest> findByStatusIn(Set<RoomBookingStatus> statuses);
    
    @Query("SELECT rbr FROM ResourceBookingRequest rbr WHERE rbr.allocatedResource.id = :resourceId AND " +
           "(rbr.status = 'APPROVED' OR rbr.status = 'CONFIRMED') AND " +
           "((rbr.event IS NOT NULL AND CAST(rbr.event.startTime AS date) = :date) OR " +
           "(rbr.meetingStart IS NOT NULL AND CAST(rbr.meetingStart AS date) = :date))")
    List<ResourceBookingRequest> findByAllocatedResourceIdAndDateBookings(@Param("resourceId") Long resourceId, @Param("date") LocalDate date);
    
    @Query("SELECT rbr FROM ResourceBookingRequest rbr WHERE rbr.allocatedResource.id = :resourceId AND " +
           "(rbr.status = 'APPROVED' OR rbr.status = 'CONFIRMED') AND " +
           "((rbr.event IS NOT NULL AND rbr.event.startTime < :endTime AND rbr.event.endTime > :startTime) OR " +
           "(rbr.meetingStart IS NOT NULL AND rbr.meetingStart < :endTime AND rbr.meetingEnd > :startTime))")
    List<ResourceBookingRequest> findConflictingBookings(@Param("resourceId") Long resourceId,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);
    
    @Query("select r from ResourceBookingRequest r where r.status = 'PENDING' and r.requestedAt <= ?1")
    List<ResourceBookingRequest> findPendingOlderThan(LocalDateTime cutoff);
    
    @Query("SELECT rbr FROM ResourceBookingRequest rbr WHERE rbr.status = 'APPROVED' AND rbr.approvedAt <= :cutoff")
    List<ResourceBookingRequest> findApprovedToConfirm(@Param("cutoff") LocalDateTime cutoff);

    List<ResourceBookingRequest> findBySplitGroupId(UUID splitGroupId);

    boolean existsByEvent_IdAndStatus(Long eventId, RoomBookingStatus status);

    boolean existsByEvent_IdAndStatusIn(Long eventId, Set<RoomBookingStatus> statuses);

    void deleteByEvent_Id(Long eventId);
    
    @Modifying
    @Query("UPDATE ResourceBookingRequest r SET r.status = 'REJECTED' WHERE r.splitGroupId = :groupId AND r.id != :approvedId AND r.status = 'PENDING'")
    void rejectSplitSiblingsBulk(@Param("groupId") UUID groupId, @Param("approvedId") Long approvedId);
    
    @Query("SELECT r FROM ResourceBookingRequest r ORDER BY r.requestedAt DESC")
    org.springframework.data.domain.Page<ResourceBookingRequest> findRecentBookings(org.springframework.data.domain.Pageable pageable);
}
