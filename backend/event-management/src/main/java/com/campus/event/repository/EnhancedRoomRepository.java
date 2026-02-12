package com.campus.event.repository;

import com.campus.event.domain.Room;
import com.campus.event.domain.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface EnhancedRoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByFloorIdOrderByRoomNumberAsc(Long floorId);
    List<Room> findByFloorIdAndIsActiveTrueOrderByRoomNumberAsc(Long floorId);
    
    @Query("SELECT r FROM Room r WHERE r.floor.building.id = :buildingId AND r.isActive = true ORDER BY r.floor.floorNumber ASC, r.roomNumber ASC")
    List<Room> findByBuildingIdAndIsActiveTrue(@Param("buildingId") Long buildingId);
    
    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity AND r.isActive = true")
    List<Room> findByCapacityGreaterThanEqual(@Param("minCapacity") Integer minCapacity);
    
    @Query("SELECT r FROM Room r WHERE r.type = :type AND r.isActive = true")
    List<Room> findByTypeAndIsActiveTrue(@Param("type") RoomType type);
    
    boolean existsByFloorIdAndRoomNumber(Long floorId, String roomNumber);
    
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r " +
           "WHERE r.id = :roomId AND " +
           "EXISTS (SELECT 1 FROM FixedTimetable ft WHERE ft.room.id = :roomId AND " +
           "ft.dayOfWeek = :dayOfWeek AND " +
           "ft.startTime < :endTime AND ft.endTime > :startTime AND ft.isActive = true)")
    boolean hasFixedTimetableConflict(@Param("roomId") Long roomId, 
                                    @Param("dayOfWeek") java.time.DayOfWeek dayOfWeek,
                                    @Param("startTime") LocalTime startTime, 
                                    @Param("endTime") LocalTime endTime);
}
