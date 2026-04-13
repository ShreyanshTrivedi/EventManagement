package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * One daily booking slot within a {@link RoomBookingRequest}.
 * <p>
 * Single-day bookings have exactly 1 slot.
 * Multi-day events have one slot per day.
 * Overnight events are split at midnight into two slots.
 * <p>
 * Each slot can be allocated to a DIFFERENT room, allowing
 * per-day room assignment for multi-day events.
 */
@Entity
@Table(name = "room_booking_slots")
public class RoomBookingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @NotNull
    private RoomBookingRequest request;

    @NotNull
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    public RoomBookingSlot() {}

    public RoomBookingSlot(RoomBookingRequest request, LocalDate slotDate,
                           LocalTime startTime, LocalTime endTime) {
        this.request = request;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and setters
    public Long getId() { return id; }
    public RoomBookingRequest getRequest() { return request; }
    public void setRequest(RoomBookingRequest request) { this.request = request; }
    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
