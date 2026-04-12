package com.campus.event.service;

import com.campus.event.domain.*;
import com.campus.event.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RoomManagementService {
    
    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;
    private final ResourceRepository resourceRepository;
    
    public RoomManagementService(BuildingRepository buildingRepository, 
                                FloorRepository floorRepository,
                                ResourceRepository resourceRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.resourceRepository = resourceRepository;
    }
    
    // Building operations
    public List<Building> getAllBuildings() {
        return buildingRepository.findByIsActiveTrue();
    }
    
    public Building createBuilding(String name, String code, String description) {
        if (buildingRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Building with code " + code + " already exists");
        }
        Building building = new Building(name, code, description);
        return buildingRepository.save(building);
    }
    
    // Floor operations
    public List<Floor> getFloorsByBuilding(Long buildingId) {
        return floorRepository.findByBuildingIdOrderByFloorNumberAsc(buildingId);
    }
    
    public Floor createFloor(Long buildingId, Integer floorNumber, String name) {
        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new IllegalArgumentException("Building not found"));
            
        if (floorRepository.existsByBuildingIdAndFloorNumber(buildingId, floorNumber)) {
            throw new IllegalArgumentException("Floor " + floorNumber + " already exists in this building");
        }
        
        Floor floor = new Floor(floorNumber, name, building);
        return floorRepository.save(floor);
    }
    
    // Room operations
    // Resource operations
    public List<Resource> getRoomsByFloor(Long floorId) {
        return resourceRepository.findByFloor_IdAndIsActiveTrueOrderByNameAsc(floorId);
    }
    
    public List<Resource> getRoomsByBuilding(Long buildingId) {
        return resourceRepository.findByBuilding_IdAndIsActiveTrueOrderByNameAsc(buildingId);
    }
    
    public Resource getRoomById(Long roomId) {
        return resourceRepository.findById(roomId).orElse(null);
    }
    
    public Resource createRoom(Long floorId, String roomNumber, String name, ResourceType type, Integer capacity, String amenities) {
        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new IllegalArgumentException("Floor not found"));
            
        // We will just use 'name' instead of combining since Resource does not have roomNumber.
        // Or if you want to prepend roomNumber: String resourceName = roomNumber + " - " + name;
        String resourceName = roomNumber + " - " + name;

        Resource resource = new Resource();
        resource.setName(resourceName);
        resource.setResourceType(type);
        resource.setCapacity(capacity);
        resource.setFloor(floor);
        resource.setBuilding(floor.getBuilding());
        resource.setAmenities(amenities);
        return resourceRepository.save(resource);
    }
    
    // Initialize building structures (operates on EXISTING buildings only — never creates new ones)
    public void initializeBuildings() {
        Building ab1 = buildingRepository.findByCode("BLD_A").orElse(null);
        Building ab2 = buildingRepository.findByCode("BLD_B").orElse(null);

        if (ab1 != null) setupBuildingFloorsAndRooms(ab1, "A", true);
        if (ab2 != null) setupBuildingFloorsAndRooms(ab2, "B", false);
    }

    private void setupBuildingFloorsAndRooms(Building building, String prefix, boolean hasAuditorium) {
        List<Floor> existingFloors = floorRepository.findByBuildingIdOrderByFloorNumberAsc(building.getId());
        if (existingFloors.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                String floorName = i == 0 ? "Ground Floor" : (i == 1 ? "First Floor" : "Second Floor");
                Floor floor = createFloor(building.getId(), i, floorName);
                createSampleRooms(floor, prefix, hasAuditorium);
            }
            return;
        }

        for (Floor floor : existingFloors) {
            if (resourceRepository.findByFloor_IdAndIsActiveTrueOrderByNameAsc(floor.getId()).isEmpty()) {
                createSampleRooms(floor, prefix, hasAuditorium);
            }
        }
    }
    
    private void createSampleRooms(Floor floor, String prefix, boolean hasAuditorium) {
        List<Resource> sampleRooms = new ArrayList<>();
        
        if (floor.getFloorNumber() == 0) { // Ground Floor
            sampleRooms.add(createSampleRoom(prefix + "0101", "Lecture Hall 1", ResourceType.ROOM, 120, floor));
            sampleRooms.add(createSampleRoom(prefix + "0102", "Lecture Hall 2", ResourceType.ROOM, 120, floor));
            sampleRooms.add(createSampleRoom(prefix + "0103", "Computer Lab 1", ResourceType.LAB, 60, floor));
            sampleRooms.add(createSampleRoom(prefix + "0104", "Computer Lab 2", ResourceType.LAB, 60, floor));
            if (hasAuditorium) {
                sampleRooms.add(createSampleRoom(prefix + "0105", "Main Auditorium", ResourceType.AUDITORIUM, 300, floor));
            }
        } else if (floor.getFloorNumber() == 1) { // First Floor
            sampleRooms.add(createSampleRoom(prefix + "0201", "Classroom 1", ResourceType.ROOM, 40, floor));
            sampleRooms.add(createSampleRoom(prefix + "0202", "Classroom 2", ResourceType.ROOM, 40, floor));
            sampleRooms.add(createSampleRoom(prefix + "0203", "Seminar Hall", ResourceType.ROOM, 80, floor));
            sampleRooms.add(createSampleRoom(prefix + "0204", "Meeting Room 1", ResourceType.ROOM, 20, floor));
            sampleRooms.add(createSampleRoom(prefix + "0205", "Meeting Room 2", ResourceType.ROOM, 20, floor));
        } else { // Second Floor
            sampleRooms.add(createSampleRoom(prefix + "0301", "Classroom 3", ResourceType.ROOM, 40, floor));
            sampleRooms.add(createSampleRoom(prefix + "0302", "Classroom 4", ResourceType.ROOM, 40, floor));
            sampleRooms.add(createSampleRoom(prefix + "0303", "Physics Lab", ResourceType.LAB, 30, floor));
            sampleRooms.add(createSampleRoom(prefix + "0304", "Chemistry Lab", ResourceType.LAB, 30, floor));
        }
        
        for (Resource room : sampleRooms) {
            room.setAmenities("Projector, Whiteboard, WiFi");
            resourceRepository.save(room);
        }
    }

    private Resource createSampleRoom(String num, String name, ResourceType type, int cap, Floor floor) {
        Resource r = new Resource();
        r.setName(num + " - " + name);
        r.setResourceType(type);
        r.setCapacity(cap);
        r.setFloor(floor);
        r.setBuilding(floor.getBuilding());
        return r;
    }
}
