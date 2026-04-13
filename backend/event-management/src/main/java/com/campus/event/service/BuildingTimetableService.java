package com.campus.event.service;

import com.campus.event.domain.BuildingTimetable;
import com.campus.event.repository.BuildingTimetableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class BuildingTimetableService {

    private final BuildingTimetableRepository buildingTimetableRepository;

    public BuildingTimetableService(BuildingTimetableRepository buildingTimetableRepository) {
        this.buildingTimetableRepository = buildingTimetableRepository;
    }

    /**
     * True when every local-time segment of [start, end] on each calendar day touched
     * lies fully inside at least one configured window for {@code buildingId}.
     * <p>
     * Handles overnight events (e.g. 15:00 → 05:00) by splitting at midnight:
     * Day 1: 15:00 → 23:59, Day 2: 00:00 → 05:00
     */
    @Transactional(readOnly = true)
    public boolean isBookingWithinBuildingHours(Long buildingId, LocalDateTime start, LocalDateTime end) {
        if (buildingId == null || start == null || end == null) {
            return true;
        }

        // Handle overnight: if end is not after start but dates differ, it's a valid overnight event
        if (!end.isAfter(start)) {
            // Same date and end <= start is truly invalid
            if (start.toLocalDate().equals(end.toLocalDate())) {
                return true; // let other validators catch this
            }
        }

        LocalDate day = start.toLocalDate();
        LocalDate last = end.toLocalDate();
        while (!day.isAfter(last)) {
            DayOfWeek dow = day.getDayOfWeek();
            LocalTime segStart = day.equals(start.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;
            LocalTime segEnd = day.equals(end.toLocalDate()) ? end.toLocalTime() : LocalTime.of(23, 59, 59, 999_999_999);
            if (!segmentCovered(buildingId, dow, segStart, segEnd)) {
                return false;
            }
            day = day.plusDays(1);
        }
        return true;
    }

    private boolean segmentCovered(Long buildingId, DayOfWeek dow, LocalTime segStart, LocalTime segEnd) {
        List<BuildingTimetable> rows = buildingTimetableRepository.findByBuilding_IdAndDayOfWeekOrderByStartTimeAsc(buildingId, dow);
        // No rows for this building/day → do not restrict (tests, legacy DBs without seed)
        if (rows.isEmpty()) {
            return true;
        }
        return rows.stream().anyMatch(r ->
                !r.getStartTime().isAfter(segStart) && !r.getEndTime().isBefore(segEnd));
    }
}
