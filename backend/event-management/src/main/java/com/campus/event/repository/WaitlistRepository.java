package com.campus.event.repository;

import com.campus.event.domain.WaitlistEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByEvent_IdAndUser_Id(Long eventId, Long userId);

    boolean existsByEvent_IdAndUser_Username(Long eventId, String username);

    /** Returns the next entry to promote (lowest position, not yet promoted). */
    @Query("SELECT w FROM WaitlistEntry w WHERE w.event.id = :eventId AND w.promotedAt IS NULL ORDER BY w.position ASC")
    List<WaitlistEntry> findPendingByEventOrdered(@Param("eventId") Long eventId);

    /** Returns position for a given user/event (0 if not on waitlist). */
    @Query("SELECT w.position FROM WaitlistEntry w WHERE w.event.id = :eventId AND w.user.username = :username AND w.promotedAt IS NULL")
    Optional<Integer> findPositionByEventAndUser(@Param("eventId") Long eventId, @Param("username") String username);

    /** Max position currently assigned for an event — used to compute next position. */
    @Query("SELECT COALESCE(MAX(w.position), 0) FROM WaitlistEntry w WHERE w.event.id = :eventId")
    int maxPositionForEvent(@Param("eventId") Long eventId);

    /** Pessimistic lock on the waitlist entry — prevents concurrent promotions. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WaitlistEntry w WHERE w.id = :id")
    Optional<WaitlistEntry> findByIdWithLock(@Param("id") Long id);

    Optional<WaitlistEntry> findByEvent_IdAndUser_Username(Long eventId, String username);

    void deleteByEvent_IdAndUser_Username(Long eventId, String username);

    /** Count of unpromoted entries (queue depth). */
    @Query("SELECT COUNT(w) FROM WaitlistEntry w WHERE w.event.id = :eventId AND w.promotedAt IS NULL")
    long countPendingByEvent(@Param("eventId") Long eventId);
}
