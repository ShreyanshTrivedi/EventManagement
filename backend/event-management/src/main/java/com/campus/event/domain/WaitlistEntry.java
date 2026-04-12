package com.campus.event.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Represents one user's position in the waitlist for a capacity-full event.
 *
 * <p>When a registration is cancelled, {@link com.campus.event.service.WaitlistService}
 * promotes the entry with the lowest {@code position} value for that event,
 * setting {@code promotedAt} and creating a real {@link EventRegistration}.
 *
 * <p>Uniqueness is enforced both at DB level (UNIQUE constraint created in V12)
 * and at service level.
 */
@Entity
@Table(
    name = "waitlist_entries",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_waitlist_event_user",
        columnNames = {"event_id", "user_id"}
    )
)
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime queuedAt = LocalDateTime.now();

    /**
     * 1-based position in the queue. Lower = promoted first.
     * Assigned as max(position)+1 at insertion time.
     */
    @Column(nullable = false)
    private int position;

    /** Set when this entry is promoted to a full registration. */
    private LocalDateTime promotedAt;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long          getId()         { return id; }
    public Event         getEvent()      { return event; }
    public void          setEvent(Event e)  { this.event = e; }
    public User          getUser()       { return user; }
    public void          setUser(User u) { this.user = u; }
    public LocalDateTime getQueuedAt()   { return queuedAt; }
    public void          setQueuedAt(LocalDateTime t) { this.queuedAt = t; }
    public int           getPosition()   { return position; }
    public void          setPosition(int p) { this.position = p; }
    public LocalDateTime getPromotedAt() { return promotedAt; }
    public void          setPromotedAt(LocalDateTime t) { this.promotedAt = t; }
}
