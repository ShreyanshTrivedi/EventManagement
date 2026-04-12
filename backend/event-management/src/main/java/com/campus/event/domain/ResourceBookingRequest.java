package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_booking_requests")
public class ResourceBookingRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref1_resource_id")
    private Resource pref1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref2_resource_id")
    private Resource pref2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref3_resource_id")
    private Resource pref3;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_resource_id")
    private Resource allocatedResource;

    @Enumerated(EnumType.STRING)
    private RoomBookingStatus status = RoomBookingStatus.PENDING;

    @NotNull
    private LocalDateTime requestedAt = LocalDateTime.now();

    private LocalDateTime approvedAt;
    private LocalDateTime confirmedAt;

    private String requestedByUsername;
    private String approvedByUsername;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claimed_by_id")
    private User claimedBy;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    // For standalone meeting bookings (no associated event)
    private LocalDateTime meetingStart;
    private LocalDateTime meetingEnd;
    private String meetingPurpose;

    /**
     * When preferences mix LARGE_HALL and NORMAL_ROOM, multiple requests share this id
     * (see {@link com.campus.event.service.RoomApprovalRules}).
     */
    @Column(name = "split_group_id")
    private UUID splitGroupId;

    // Helper methods for schedule management
    public java.time.LocalDate getDate() {
        if (event != null && event.getStartTime() != null) {
            return event.getStartTime().toLocalDate();
        }
        if (meetingStart != null) {
            return meetingStart.toLocalDate();
        }
        return requestedAt.toLocalDate();
    }
    
    public java.time.LocalTime getStartTime() {
        if (event != null && event.getStartTime() != null) {
            return event.getStartTime().toLocalTime();
        }
        if (meetingStart != null) {
            return meetingStart.toLocalTime();
        }
        return requestedAt.toLocalTime();
    }
    
    public java.time.LocalTime getEndTime() {
        if (event != null && event.getEndTime() != null) {
            return event.getEndTime().toLocalTime();
        }
        if (meetingEnd != null) {
            return meetingEnd.toLocalTime();
        }
        return requestedAt.toLocalTime().plusHours(1);
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public Resource getPref1() { return pref1; }
    public void setPref1(Resource pref1) { this.pref1 = pref1; }
    public Resource getPref2() { return pref2; }
    public void setPref2(Resource pref2) { this.pref2 = pref2; }
    public Resource getPref3() { return pref3; }
    public void setPref3(Resource pref3) { this.pref3 = pref3; }
    public Resource getAllocatedResource() { return allocatedResource; }
    public void setAllocatedResource(Resource allocatedResource) { this.allocatedResource = allocatedResource; }
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
    public User getClaimedBy() { return claimedBy; }
    public void setClaimedBy(User claimedBy) { this.claimedBy = claimedBy; }
    public LocalDateTime getClaimedAt() { return claimedAt; }
    public void setClaimedAt(LocalDateTime claimedAt) { this.claimedAt = claimedAt; }
    public LocalDateTime getMeetingStart() { return meetingStart; }
    public void setMeetingStart(LocalDateTime meetingStart) { this.meetingStart = meetingStart; }
    public LocalDateTime getMeetingEnd() { return meetingEnd; }
    public void setMeetingEnd(LocalDateTime meetingEnd) { this.meetingEnd = meetingEnd; }
    public String getMeetingPurpose() { return meetingPurpose; }
    public void setMeetingPurpose(String meetingPurpose) { this.meetingPurpose = meetingPurpose; }
    public UUID getSplitGroupId() { return splitGroupId; }
    public void setSplitGroupId(UUID splitGroupId) { this.splitGroupId = splitGroupId; }
}
