package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_booking_requests")
public class RoomBookingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref1_room_id")
    private Room pref1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref2_room_id")
    private Room pref2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref3_room_id")
    private Room pref3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_room_id")
    private Room allocatedRoom;

    @Enumerated(EnumType.STRING)
    private RoomBookingStatus status = RoomBookingStatus.PENDING;

    @NotNull
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime approvedAt;
    private LocalDateTime confirmedAt;

    private String requestedByUsername;
    private String approvedByUsername;

    // For standalone meeting bookings (no associated event)
    private LocalDateTime meetingStart;
    private LocalDateTime meetingEnd;
    private String meetingPurpose;

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Room getPref1() { return pref1; }
    public void setPref1(Room pref1) { this.pref1 = pref1; }
    public Room getPref2() { return pref2; }
    public void setPref2(Room pref2) { this.pref2 = pref2; }
    public Room getPref3() { return pref3; }
    public void setPref3(Room pref3) { this.pref3 = pref3; }
    public Room getAllocatedRoom() { return allocatedRoom; }
    public void setAllocatedRoom(Room allocatedRoom) { this.allocatedRoom = allocatedRoom; }
    public RoomBookingStatus getStatus() { return status; }
    public void setStatus(RoomBookingStatus status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getRequestedByUsername() { return requestedByUsername; }
    public void setRequestedByUsername(String requestedByUsername) { this.requestedByUsername = requestedByUsername; }
    public String getApprovedByUsername() { return approvedByUsername; }
    public void setApprovedByUsername(String approvedByUsername) { this.approvedByUsername = approvedByUsername; }
    public LocalDateTime getMeetingStart() { return meetingStart; }
    public void setMeetingStart(LocalDateTime meetingStart) { this.meetingStart = meetingStart; }
    public LocalDateTime getMeetingEnd() { return meetingEnd; }
    public void setMeetingEnd(LocalDateTime meetingEnd) { this.meetingEnd = meetingEnd; }
    public String getMeetingPurpose() { return meetingPurpose; }
    public void setMeetingPurpose(String meetingPurpose) { this.meetingPurpose = meetingPurpose; }
}
