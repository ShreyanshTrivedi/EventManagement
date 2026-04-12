package com.campus.event.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * A bookable resource — the unified abstraction for anything that can be reserved:
 * rooms, open spaces, labs, auditoriums, sports grounds, cafeteria sections.
 *
 * <p>Existing {@link Room} rows are migrated into this table by V12 with
 * {@code resourceType = ROOM} and {@code roomRefId} pointing back to the original row.
 * New open-space/outdoor resources will have a {@code null} roomRefId.
 *
 * <p>Availability is enforced via the {@code excl_bookings_resource_no_overlap}
 * PostgreSQL EXCLUSION constraint on {@code bookings.resource_id}.
 */
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType = ResourceType.ROOM;

    private Integer capacity;

    /**
     * The building this resource belongs to.
     * May be {@code null} for campus-wide open-air resources.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    /**
     * The floor within the building (null for outdoor/open-space types).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    private Floor floor;

    private boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Back-link to the original {@link Room} row for resources migrated in V12.
     * {@code null} for newly created open-space resources.
     */
    @Column(name = "room_ref_id")
    private Long roomRefId;

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long   getId()           { return id; }
    public String getName()         { return name; }
    public void   setName(String n) { this.name = n; }

    public ResourceType getResourceType()           { return resourceType; }
    public void         setResourceType(ResourceType t) { this.resourceType = t; }

    public Integer getCapacity()              { return capacity; }
    public void    setCapacity(Integer c)     { this.capacity = c; }

    public Building getBuilding()              { return building; }
    public void     setBuilding(Building b)   { this.building = b; }

    public Floor getFloor()            { return floor; }
    public void  setFloor(Floor f)     { this.floor = f; }

    public boolean isActive()             { return isActive; }
    public void    setActive(boolean a)   { this.isActive = a; }

    public String getAmenities()          { return amenities; }
    public void   setAmenities(String a)  { this.amenities = a; }

    public String getDescription()            { return description; }
    public void   setDescription(String d)    { this.description = d; }

    public Long getRoomRefId()         { return roomRefId; }
    public void setRoomRefId(Long id)  { this.roomRefId = id; }
}
