package com.campus.event.domain;

/**
 * Classifies a bookable resource.
 *
 * <ul>
 *   <li>{@code ROOM}         – physical enclosed room (classroom, office, etc.)
 *   <li>{@code AUDITORIUM}   – large-capacity hall managed at building level
 *   <li>{@code LAB}          – equipped computer / science lab
 *   <li>{@code OPEN_SPACE}   – outdoor / semi-outdoor area (amphitheatre, lawn, etc.)
 *   <li>{@code SPORTS_GROUND}– playing field, court, or sports facility
 *   <li>{@code CAFETERIA}    – food court area bookable for events
 * </ul>
 */
public enum ResourceType {
    ROOM,
    AUDITORIUM,
    LAB,
    OPEN_SPACE,
    SPORTS_GROUND,
    CAFETERIA
}
