package com.campus.event.repository;

import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    List<Resource> findByIsActiveTrueOrderByNameAsc();

    List<Resource> findByBuilding_IdAndIsActiveTrueOrderByNameAsc(Long buildingId);

    List<Resource> findByResourceTypeAndIsActiveTrueOrderByNameAsc(ResourceType type);

    List<Resource> findByBuilding_IdAndResourceTypeAndIsActiveTrueOrderByNameAsc(
            Long buildingId, ResourceType type);

    List<Resource> findByFloor_IdAndIsActiveTrueOrderByNameAsc(Long floorId);

    Optional<Resource> findByNameAndFloor_Id(String name, Long floorId);

    /** Used to locate the Resource counterpart of a legacy Room during migration / availability checks. */
    Optional<Resource> findByRoomRefId(Long roomId);

    /** Pessimistic write lock — used during smart suggestion to prevent concurrent booking of the same resource. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resource r WHERE r.id = :id")
    Optional<Resource> findByIdWithLock(@Param("id") Long id);
}
