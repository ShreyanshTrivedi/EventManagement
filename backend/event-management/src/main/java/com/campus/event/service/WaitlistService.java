package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.EventRegistration;
import com.campus.event.domain.EventStatus;
import com.campus.event.domain.Registration;
import com.campus.event.domain.User;
import com.campus.event.domain.WaitlistEntry;
import com.campus.event.repository.EventRegistrationRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.RegistrationRepository;
import com.campus.event.repository.WaitlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Manages the waitlist queue for capacity-full events.
 *
 * <h3>Flow</h3>
 * <pre>
 *   register() → capacity full → joinWaitlist()
 *   unregister() → slot opens  → promoteNext()
 * </pre>
 *
 * <h3>Concurrency</h3>
 * All mutating methods are {@code @Transactional}. Promotion acquires a
 * pessimistic write-lock on the {@link WaitlistEntry} row to prevent two
 * concurrent cancellations from promoting the same person.
 */
@Service
public class WaitlistService {
    private static final Logger log = LoggerFactory.getLogger(WaitlistService.class);

    private final WaitlistRepository waitlistRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final RegistrationRepository legacyRegistrationRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    public WaitlistService(WaitlistRepository waitlistRepository,
                           EventRegistrationRepository eventRegistrationRepository,
                           RegistrationRepository legacyRegistrationRepository,
                           EventRepository eventRepository,
                           NotificationService notificationService) {
        this.waitlistRepository = waitlistRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.legacyRegistrationRepository = legacyRegistrationRepository;
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
    }

    /**
     * Adds a user to the waitlist for an event.
     *
     * @return the 1-based position in the queue
     * @throws IllegalStateException if already on waitlist or registered
     */
    @Transactional
    public int joinWaitlist(Event event, User user) {
        // Guard: already registered
        if (eventRegistrationRepository.existsByEvent_IdAndUser_Username(
                event.getId(), user.getUsername())) {
            throw new IllegalStateException("Already registered for this event");
        }
        // Guard: already on waitlist
        if (waitlistRepository.existsByEvent_IdAndUser_Username(
                event.getId(), user.getUsername())) {
            return waitlistRepository
                    .findPositionByEventAndUser(event.getId(), user.getUsername())
                    .orElse(-1);
        }

        int nextPosition = waitlistRepository.maxPositionForEvent(event.getId()) + 1;

        WaitlistEntry entry = new WaitlistEntry();
        entry.setEvent(event);
        entry.setUser(user);
        entry.setPosition(nextPosition);
        waitlistRepository.save(entry);

        log.info("User '{}' joined waitlist for event {} at position {}",
                user.getUsername(), event.getId(), nextPosition);
        return nextPosition;
    }

    /**
     * Removes a user from the waitlist (called when they cancel their waitlist spot).
     */
    @Transactional
    public void leaveWaitlist(Long eventId, String username) {
        waitlistRepository.deleteByEvent_IdAndUser_Username(eventId, username);
    }

    /**
     * Returns the 1-based waitlist position for the user, or empty if not on the list.
     */
    @Transactional(readOnly = true)
    public Optional<Integer> getPosition(Long eventId, String username) {
        return waitlistRepository.findPositionByEventAndUser(eventId, username);
    }

    /**
     * Promotes the top-of-queue waitlist entry to a full registration.
     *
     * <p>Called after a registration is cancelled. Uses pessimistic locking on
     * the waitlist entry to prevent two concurrent cancellations from promoting
     * the same user.
     *
     * @return the promoted user (for notification), or empty if waitlist is empty
     */
    @Transactional
    public Optional<User> promoteNext(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return Optional.empty();

        // Quick guard: event must still be in a registrable state
        if (event.getStatus() == EventStatus.COMPLETED) {
            log.debug("Event {} is COMPLETED — skipping waitlist promotion", eventId);
            return Optional.empty();
        }

        List<WaitlistEntry> queue = waitlistRepository.findPendingByEventOrdered(eventId);
        if (queue.isEmpty()) return Optional.empty();

        WaitlistEntry top = queue.get(0);

        // Acquire pessimistic lock on this entry to prevent double-promotion
        WaitlistEntry locked = waitlistRepository.findByIdWithLock(top.getId()).orElse(null);
        if (locked == null || locked.getPromotedAt() != null) {
            // Already promoted by a concurrent transaction
            return Optional.empty();
        }

        User user = locked.getUser();

        // Create the EventRegistration
        EventRegistration reg = new EventRegistration();
        reg.setEvent(event);
        reg.setUser(user);
        eventRegistrationRepository.save(reg);

        // Sync with legacy registrations table (best-effort)
        try {
            if (user.getEmail() != null
                    && !legacyRegistrationRepository.existsByEventIdAndEmail(
                            eventId, user.getEmail())) {
                Registration legacy = new Registration();
                legacy.setEvent(event);
                legacy.setEmail(user.getEmail());
                legacy.setFullName(user.getFullName() != null
                        ? user.getFullName() : user.getUsername());
                legacyRegistrationRepository.save(legacy);
            }
        } catch (Exception ignored) { /* best-effort */ }

        // Mark entry as promoted (history preserved — row not deleted)
        locked.setPromotedAt(LocalDateTime.now());
        waitlistRepository.save(locked);

        log.info("Promoted user '{}' from waitlist to registration for event {}",
                user.getUsername(), eventId);

        // Notify the promoted user (async — won't block caller)
        String subj = "You're in! Registration confirmed: " + event.getTitle();
        String msg  = "A spot opened up and you've been promoted from the waitlist for '"
                + event.getTitle() + "' on " + event.getStartTime().toLocalDate()
                + ". You are now officially registered!";
        notificationService.notifyAllChannels(user, subj, msg);

        return Optional.of(user);
    }

    /** Returns the depth of the unpromoted queue for an event. */
    @Transactional(readOnly = true)
    public long queueDepth(Long eventId) {
        return waitlistRepository.countPendingByEvent(eventId);
    }
}
