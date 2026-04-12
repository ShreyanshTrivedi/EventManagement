package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A confirmed direct booking of a {@link Resource} (or legacy {@link Room}).
 *
 * <p>New bookings (post-V12) always populate {@code resource}.
 * Legacy bookings (pre-V12) have {@code room} only; their {@code resource_id}
 * is backfilled by the V12 migration. Conflict prevention is enforced by the
 * {@code excl_bookings_resource_no_overlap} PostgreSQL EXCLUDE constraint.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() { return id; }


    /**
     * Unified resource FK — used for all new bookings.
     * The EXCLUDE constraint on this column is the authoritative overlap guard.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time")
    private LocalDateTime endTime;

    private String purpose;

    // ── Explicit getters/setters (Lombok covers them but kept for clarity) ────


    public Resource getResource()           { return resource; }
    public void     setResource(Resource r) { this.resource = r; }

    public User     getUser()       { return user; }
    public void     setUser(User u) { this.user = u; }

    public LocalDateTime getStartTime()             { return startTime; }
    public void          setStartTime(LocalDateTime t) { this.startTime = t; }

    public LocalDateTime getEndTime()               { return endTime; }
    public void          setEndTime(LocalDateTime t)  { this.endTime = t; }

    public String getPurpose()          { return purpose; }
    public void   setPurpose(String p)  { this.purpose = p; }
}
