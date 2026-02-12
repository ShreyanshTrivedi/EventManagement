package com.campus.event.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "floors")
public class Floor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer floorNumber;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @OneToMany(mappedBy = "floor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Room> rooms = new ArrayList<>();

    // Explicit default constructor for Hibernate
    public Floor() {
    }

    public Floor(Integer floorNumber, String name, Building building) {
        this.floorNumber = floorNumber;
        this.name = name;
        this.building = building;
    }

    public void addRoom(Room room) {
        rooms.add(room);
        room.setFloor(this);
    }

    public void removeRoom(Room room) {
        rooms.remove(room);
        room.setFloor(null);
    }
    
    // Explicit getters and setters to ensure they exist
    public Long getId() { return id; }
    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Building getBuilding() { return building; }
    public void setBuilding(Building building) { this.building = building; }
    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
}
