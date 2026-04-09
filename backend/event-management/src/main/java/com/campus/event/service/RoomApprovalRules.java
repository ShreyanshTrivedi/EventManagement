package com.campus.event.service;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomType;

/**
 * Maps physical {@link RoomType} values to approval lanes ({@link AdminScope}).
 * <p>
 * <strong>Mixed room-type preferences (OPTION 1):</strong> when a club submits three
 * preferences that span both LARGE_HALL and NORMAL_ROOM categories, the backend
 * creates <strong>multiple</strong> {@code RoomBookingRequest} rows sharing the same
 * {@code splitGroupId}. Each row only contains preferences for one category.
 * When <strong>any</strong> of those rows is approved, all other PENDING rows in the
 * same group are automatically rejected so exactly one room allocation wins.
 */
public final class RoomApprovalRules {

    private RoomApprovalRules() {
    }

    public static AdminScope scopeForRoomType(RoomType type) {
        if (type == null) {
            return AdminScope.NORMAL_ROOM;
        }
        return switch (type) {
            case AUDITORIUM, SEMINAR_HALL, LECTURE_HALL -> AdminScope.LARGE_HALL;
            case LAB, MEETING_ROOM, CLASSROOM -> AdminScope.NORMAL_ROOM;
        };
    }

    public static AdminScope scopeForRoom(Room room) {
        return room == null ? AdminScope.NORMAL_ROOM : scopeForRoomType(room.getType());
    }

    public static boolean matchesScope(Room room, AdminScope scope) {
        return scopeForRoom(room) == scope;
    }
}
