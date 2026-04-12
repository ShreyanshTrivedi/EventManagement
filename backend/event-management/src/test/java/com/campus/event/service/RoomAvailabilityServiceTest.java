package com.campus.event.service;

import com.campus.event.domain.Event;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.ResourceType;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.FixedTimetableRepository;
import com.campus.event.repository.ResourceBookingRequestRepository;
import com.campus.event.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomAvailabilityServiceTest {

    @Mock
    private ResourceBookingRequestRepository requestRepo;

    @Mock
    private FixedTimetableRepository fixedTimetableRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private RoomAvailabilityService availabilityService;

    private Resource resource1;
    private Resource resource2;

    @BeforeEach
    void setUp() {
        resource1 = mock(Resource.class);
        when(resource1.getId()).thenReturn(1L);
        when(resource1.getResourceType()).thenReturn(ResourceType.ROOM);
        when(resource1.getName()).thenReturn("R101");

        resource2 = mock(Resource.class);
        when(resource2.getId()).thenReturn(2L);
        when(resource2.getResourceType()).thenReturn(ResourceType.ROOM);
        when(resource2.getName()).thenReturn("R102");
    }

    @Test
    void isResourceAvailable_noConflicts_returnsTrue() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(fixedTimetableRepository.existsConflictingClass(any(), any(), any(), any())).thenReturn(false);
        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of());
        when(bookingRepository.findOverlappingByResource(eq(1L), any(), any())).thenReturn(List.of());

        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        assertTrue(availabilityService.isResourceAvailable(1L, start, end));
    }

    @Test
    void isResourceAvailable_overlappingBooking_returnsFalse() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(fixedTimetableRepository.existsConflictingClass(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findOverlappingByResource(eq(1L), any(), any())).thenReturn(List.of());

        LocalDateTime bookingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime bookingEnd = bookingStart.plusHours(2);

        ResourceBookingRequest existing = new ResourceBookingRequest();
        existing.setAllocatedResource(resource1);
        existing.setMeetingStart(bookingStart);
        existing.setMeetingEnd(bookingEnd);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        LocalDateTime queryStart = bookingStart.plusMinutes(30);
        LocalDateTime queryEnd = bookingEnd.plusMinutes(30);

        assertFalse(availabilityService.isResourceAvailable(1L, queryStart, queryEnd));
    }

    @Test
    void isResourceAvailable_differentResource_returnsTrue() {
        when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource2));
        when(fixedTimetableRepository.existsConflictingClass(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findOverlappingByResource(eq(2L), any(), any())).thenReturn(List.of());

        LocalDateTime bookingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime bookingEnd = bookingStart.plusHours(2);

        ResourceBookingRequest existing = new ResourceBookingRequest();
        existing.setAllocatedResource(resource1);
        existing.setMeetingStart(bookingStart);
        existing.setMeetingEnd(bookingEnd);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        assertTrue(availabilityService.isResourceAvailable(2L, bookingStart, bookingEnd));
    }

    @Test
    void availabilityForResources_mixedResults() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource2));
        when(fixedTimetableRepository.existsConflictingClass(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findOverlappingByResource(anyLong(), any(), any())).thenReturn(List.of());

        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime end = start.plusHours(2);

        ResourceBookingRequest existing = new ResourceBookingRequest();
        existing.setAllocatedResource(resource1);
        existing.setMeetingStart(start);
        existing.setMeetingEnd(end);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        Map<Long, Boolean> result = availabilityService.availabilityForResources(
                List.of(1L, 2L), start, end);

        assertFalse(result.get(1L));
        assertTrue(result.get(2L));
    }

    @Test
    void isResourceAvailable_nonOverlappingTime_returnsTrue() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(fixedTimetableRepository.existsConflictingClass(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findOverlappingByResource(eq(1L), any(), any())).thenReturn(List.of());

        LocalDateTime bookingStart = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime bookingEnd = bookingStart.plusHours(2);

        ResourceBookingRequest existing = new ResourceBookingRequest();
        existing.setAllocatedResource(resource1);
        existing.setMeetingStart(bookingStart);
        existing.setMeetingEnd(bookingEnd);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        LocalDateTime queryStart = bookingStart.plusHours(4);
        LocalDateTime queryEnd = queryStart.plusHours(2);

        assertTrue(availabilityService.isResourceAvailable(1L, queryStart, queryEnd));
    }

    @Test
    void isResourceAvailable_eventOverlap_returnsFalse() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(fixedTimetableRepository.existsConflictingClass(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findOverlappingByResource(eq(1L), any(), any())).thenReturn(List.of());

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(9);
        LocalDateTime end = start.plusHours(2);

        Event ev = new Event();
        ev.setStartTime(start);
        ev.setEndTime(end);

        ResourceBookingRequest existing = new ResourceBookingRequest();
        existing.setAllocatedResource(resource1);
        existing.setEvent(ev);
        existing.setStatus(RoomBookingStatus.APPROVED);

        when(requestRepo.findByStatusIn(Set.of(RoomBookingStatus.APPROVED, RoomBookingStatus.CONFIRMED)))
                .thenReturn(List.of(existing));

        assertFalse(availabilityService.isResourceAvailable(1L, start.plusMinutes(30), end.plusMinutes(30)));
    }
}
