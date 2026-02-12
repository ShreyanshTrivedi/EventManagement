package com.campus.event.repository;

import com.campus.event.domain.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBuildingIdOrderByFloorNumberAsc(Long buildingId);
    
    @Query("SELECT f FROM Floor f WHERE f.building.id = :buildingId AND f.floorNumber = :floorNumber")
    Floor findByBuildingIdAndFloorNumber(@Param("buildingId") Long buildingId, @Param("floorNumber") Integer floorNumber);
    
    boolean existsByBuildingIdAndFloorNumber(Long buildingId, Integer floorNumber);
}
