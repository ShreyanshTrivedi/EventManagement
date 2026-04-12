package com.campus.event.domain;

/**
 * Lifecycle states for an {@link Event}.
 *
 * <pre>
 *   DRAFT ──► PENDING ──► APPROVED ──► COMPLETED
 *                │
 *                └──► REJECTED  (future: admin rejects without room)
 * </pre>
 *
 * <ul>
 *   <li>{@code DRAFT}     – saved but not yet submitted for room approval.
 *   <li>{@code PENDING}   – room booking request submitted; awaiting admin.
 *   <li>{@code APPROVED}  – a room has been allocated; event is publicly visible.
 *   <li>{@code COMPLETED} – event end time has passed (set by scheduler).
 *   <li>{@code REJECTED}  – reserved for future explicit admin rejection flow.
 * </ul>
 */
public enum EventStatus {
    DRAFT,
    PENDING,
    APPROVED,
    COMPLETED,
    REJECTED
}
