package com.campus.event.domain;

/**
 * Building admins are scoped to approve only rooms in this category.
 * Must be set (non-null) when the user has {@link Role#BUILDING_ADMIN}.
 */
public enum AdminScope {
    LARGE_HALL,
    NORMAL_ROOM
}
