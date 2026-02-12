package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "buildings")
public class Building {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    private String code;

    private String description;

    private boolean isActive = true;

    @OneToMany(mappedBy = "building", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Floor> floors = new ArrayList<>();

    // Explicit default constructor for Hibernate
    public Building() {
    }

    public Building(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
    }

    public void addFloor(Floor floor) {
        floors.add(floor);
        floor.setBuilding(this);
    }

    public void removeFloor(Floor floor) {
        floors.remove(floor);
        floor.setBuilding(null);
    }
    
    // Explicit getters and setters to ensure they exist
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public List<Floor> getFloors() { return floors; }
    public void setFloors(List<Floor> floors) { this.floors = floors; }
}
