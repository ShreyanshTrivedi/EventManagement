package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String roomNumber;

    private String name;

    @Enumerated(EnumType.STRING)
    private RoomType type;

    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    private Floor floor;

    private String amenities;

    private boolean isActive = true;

    // Explicit default constructor for Hibernate
    public Room() {
    }

    public Room(String roomNumber, String name, RoomType type, Integer capacity, Floor floor) {
        this.roomNumber = roomNumber;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.floor = floor;
    }

    // Explicit getters and setters to avoid relying solely on Lombok processing
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Floor getFloor() { return floor; }
    public void setFloor(Floor floor) { this.floor = floor; }

    public String getAmenities() { return amenities; }
    public void setAmenities(String amenities) { this.amenities = amenities; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}


