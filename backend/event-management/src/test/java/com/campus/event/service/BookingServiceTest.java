package com.campus.event.service;

import com.campus.event.domain.Booking;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceType;
import com.campus.event.domain.User;
import com.campus.event.repository.BookingRepository;
import com.campus.event.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private BookingService bookingService;

    private Resource roomResource;
    private User user;

    @BeforeEach
    void setUp() {
        roomResource = mock(Resource.class);
        when(roomResource.getId()).thenReturn(1L);
        when(roomResource.getName()).thenReturn("Conference Room A");
        when(roomResource.getResourceType()).thenReturn(ResourceType.ROOM);

        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @Test
    void createBooking_success() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(roomResource));
        when(bookingRepository.findOverlappingByResource(eq(1L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.createBooking(1L, user, start, end, "Team meeting");

        assertNotNull(result);
        assertEquals(roomResource, result.getResource());
        assertEquals(user, result.getUser());
        assertEquals(start, result.getStartTime());
        assertEquals(end, result.getEndTime());
        assertEquals("Team meeting", result.getPurpose());
    }

    @Test
    void createBooking_resourceNotFound_throwsNotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                bookingService.createBooking(99L, user,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        "Purpose"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createBooking_conflictingTime_throwsConflict() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        Booking existingBooking = new Booking();
        existingBooking.setResource(roomResource);
        existingBooking.setStartTime(start);
        existingBooking.setEndTime(end);

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(roomResource));
        when(bookingRepository.findOverlappingByResource(eq(1L), eq(start), eq(end)))
                .thenReturn(List.of(existingBooking));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                bookingService.createBooking(1L, user, start, end, "Meeting"));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertNotNull(ex.getReason());
        assertTrue(ex.getReason().contains("booked"));
    }

    @Test
    void createBooking_openSpaceMeeting_throwsBadRequest() {
        Resource open = mock(Resource.class);
        when(open.getId()).thenReturn(2L);
        when(open.getResourceType()).thenReturn(ResourceType.OPEN_SPACE);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusHours(2);

        when(resourceRepository.findById(2L)).thenReturn(Optional.of(open));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                bookingService.createBooking(2L, user, start, end, "Outdoor"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
