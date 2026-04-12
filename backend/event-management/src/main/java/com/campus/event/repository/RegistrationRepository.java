package com.campus.event.repository;

import com.campus.event.domain.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    boolean existsByEventIdAndEmail(Long eventId, String email);
    List<Registration> findByEmail(String email);
    void deleteByEvent_Id(Long eventId);

    /** Removes a specific legacy registration by event + email (used during cancel-registration). */
    @Modifying
    @Query("DELETE FROM Registration r WHERE r.event.id = :eventId AND r.email = :email")
    void deleteByEventIdAndEmail(@Param("eventId") Long eventId, @Param("email") String email);
}
