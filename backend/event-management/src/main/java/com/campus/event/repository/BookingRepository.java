package com.campus.event.repository;

import com.campus.event.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Finds overlapping bookings by RESOURCE (new unified model).
     * Half-open interval: [start, end) — strict < and > so adjacent
     * bookings sharing an exact boundary are NOT flagged as conflicts.
     */
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.startTime < :end AND b.endTime > :start")
    List<Booking> findOverlappingByResource(@Param("resourceId") Long resourceId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);


}
