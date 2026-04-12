package com.campus.event.repository;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Public events that are fully approved — visible on student dashboard. */
    @Query("SELECT e FROM Event e WHERE e.isPublic = true AND e.status = 'APPROVED'")
    List<Event> findByIsPublicTrue();

    /** All events visible to the owner (any status). */
    List<Event> findByCreatedBy_Username(String username);

    boolean existsByTitleAndStartTimeAndCreatedBy_Id(String title, LocalDateTime startTime, Long userId);

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e " +
           "WHERE e.createdBy.id = :userId AND e.startTime < :endTime AND e.endTime > :startTime")
    boolean hasOverlappingEvents(@Param("userId") Long userId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    /** Pessimistic write lock — used inside the registration capacity transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    /** Bulk-transition APPROVED events whose end time has passed to COMPLETED. */
    @Modifying
    @Query("UPDATE Event e SET e.status = 'COMPLETED' " +
           "WHERE e.status = 'APPROVED' AND e.endTime < :now")
    int markCompletedIfPast(@Param("now") LocalDateTime now);
}



