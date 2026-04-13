package com.campus.event.repository;

import com.campus.event.domain.RoomBookingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface RoomBookingSlotRepository extends JpaRepository<RoomBookingSlot, Long> {

    List<RoomBookingSlot> findByRequestIdOrderBySlotDateAsc(Long requestId);

    /**
     * Finds slots that conflict with a proposed booking window on a given room and date.
     * A conflict exists when the existing slot overlaps in time with the proposed window
     * AND the slot status is APPROVED or CONFIRMED.
     */
    @Query("SELECT s FROM RoomBookingSlot s WHERE s.room.id = :roomId " +
           "AND s.slotDate = :date " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND s.status IN ('APPROVED', 'CONFIRMED')")
    List<RoomBookingSlot> findConflictingSlots(@Param("roomId") Long roomId,
                                               @Param("date") LocalDate date,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("endTime") LocalTime endTime);

    /**
     * Returns all approved/confirmed slots for a given room on a given date.
     * Used for schedule views.
     */
    @Query("SELECT s FROM RoomBookingSlot s WHERE s.room.id = :roomId " +
           "AND s.slotDate = :date " +
           "AND s.status IN ('APPROVED', 'CONFIRMED') " +
           "ORDER BY s.startTime ASC")
    List<RoomBookingSlot> findByRoomIdAndSlotDate(@Param("roomId") Long roomId,
                                                   @Param("date") LocalDate date);

    void deleteByRequestId(Long requestId);
}
