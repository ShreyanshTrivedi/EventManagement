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
    private final RoomRepository roomRepository;
    
    public RoomManagementService(BuildingRepository buildingRepository, 
                                FloorRepository floorRepository,
                                RoomRepository roomRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
        this.roomRepository = roomRepository;
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
    public List<Room> getRoomsByFloor(Long floorId) {
        return roomRepository.findByFloorIdAndIsActiveTrueOrderByRoomNumberAsc(floorId);
    }
    
    public List<Room> getRoomsByBuilding(Long buildingId) {
        return roomRepository.findByBuildingIdAndIsActiveTrue(buildingId);
    }
    
    public Room getRoomById(Long roomId) {
        return roomRepository.findById(roomId).orElse(null);
    }
    
    public Room createRoom(Long floorId, String roomNumber, String name, RoomType type, Integer capacity, String amenities) {
        Floor floor = floorRepository.findById(floorId)
            .orElseThrow(() -> new IllegalArgumentException("Floor not found"));
            
        if (roomRepository.existsByFloorIdAndRoomNumber(floorId, roomNumber)) {
            throw new IllegalArgumentException("Room " + roomNumber + " already exists on this floor");
        }
        
        Room room = new Room(roomNumber, name, type, capacity, floor);
        room.setAmenities(amenities);
        return roomRepository.save(room);
    }
    
    // Initialize default building structure
    public void initializeDefaultBuilding() {
        Building mainBuilding = null;

        if (buildingRepository.count() == 0) {
            mainBuilding = createBuilding("RLHC (Main Building)", "RLHC", "Main College Building");
        } else {
            List<Building> buildings = buildingRepository.findByIsActiveTrue();
            if (!buildings.isEmpty()) {
                mainBuilding = buildings.get(0);
            }
        }

        if (mainBuilding == null) {
            return;
        }

        if (roomRepository.count() > 0) {
            return;
        }

        List<Floor> existingFloors = floorRepository.findByBuildingIdOrderByFloorNumberAsc(mainBuilding.getId());
        if (existingFloors.isEmpty()) {
            for (int i = 0; i < 3; i++) {
                String floorName = i == 0 ? "Ground Floor" : (i == 1 ? "First Floor" : "Second Floor");
                Floor floor = createFloor(mainBuilding.getId(), i, floorName);
                createSampleRooms(floor);
            }
            return;
        }

        for (Floor floor : existingFloors) {
            if (roomRepository.findByFloorIdOrderByRoomNumberAsc(floor.getId()).isEmpty()) {
                createSampleRooms(floor);
            }
        }
    }
    
    private void createSampleRooms(Floor floor) {
        List<Room> sampleRooms = new ArrayList<>();
        
        if (floor.getFloorNumber() == 0) { // Ground Floor
            sampleRooms.add(new Room("RLHC0101", "Lecture Hall 1", RoomType.LECTURE_HALL, 120, floor));
            sampleRooms.add(new Room("RLHC0102", "Lecture Hall 2", RoomType.LECTURE_HALL, 120, floor));
            sampleRooms.add(new Room("RLHC0103", "Computer Lab 1", RoomType.LAB, 60, floor));
            sampleRooms.add(new Room("RLHC0104", "Computer Lab 2", RoomType.LAB, 60, floor));
        } else if (floor.getFloorNumber() == 1) { // First Floor
            sampleRooms.add(new Room("RLHC0201", "Classroom 1", RoomType.CLASSROOM, 40, floor));
            sampleRooms.add(new Room("RLHC0202", "Classroom 2", RoomType.CLASSROOM, 40, floor));
            sampleRooms.add(new Room("RLHC0203", "Seminar Hall", RoomType.SEMINAR_HALL, 80, floor));
            sampleRooms.add(new Room("RLHC0204", "Meeting Room 1", RoomType.MEETING_ROOM, 20, floor));
            sampleRooms.add(new Room("RLHC0205", "Meeting Room 2", RoomType.MEETING_ROOM, 20, floor));
        } else { // Second Floor
            sampleRooms.add(new Room("RLHC0301", "Classroom 3", RoomType.CLASSROOM, 40, floor));
            sampleRooms.add(new Room("RLHC0302", "Classroom 4", RoomType.CLASSROOM, 40, floor));
            sampleRooms.add(new Room("RLHC0303", "Physics Lab", RoomType.LAB, 30, floor));
            sampleRooms.add(new Room("RLHC0304", "Chemistry Lab", RoomType.LAB, 30, floor));
        }
        
        for (Room room : sampleRooms) {
            room.setAmenities("Projector, Whiteboard, WiFi");
            roomRepository.save(room);
        }
    }
}
