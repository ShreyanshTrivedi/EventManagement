package com.campus.event.repository;

import com.campus.event.domain.BuildingTimetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface BuildingTimetableRepository extends JpaRepository<BuildingTimetable, Long> {

    List<BuildingTimetable> findByBuilding_IdAndDayOfWeekOrderByStartTimeAsc(Long buildingId, DayOfWeek dayOfWeek);
}
