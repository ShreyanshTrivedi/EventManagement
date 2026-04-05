package com.campus.event.service;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Event;
import com.campus.event.domain.Room;
import com.campus.event.domain.RoomBookingRequest;
import com.campus.event.domain.RoomBookingStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EventRoomBookingSplitService {

    /**
     * One homogeneous request, or several (same {@link UUID} {@code splitGroupId}) when
     * preferences mix {@link AdminScope} values — see {@link RoomApprovalRules}.
     */
    public List<RoomBookingRequest> buildEventRequests(Event event, Room r1, Room r2, Room r3, String requestedBy) {
        List<Room> prefs = List.of(r1, r2, r3);
        Map<AdminScope, List<Room>> grouped = new LinkedHashMap<>();
        for (Room r : prefs) {
            AdminScope s = RoomApprovalRules.scopeForRoom(r);
            grouped.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
        }

        if (grouped.size() == 1) {
            RoomBookingRequest rr = baseRequest(event, requestedBy, null);
            rr.setPref1(r1);
            rr.setPref2(r2);
            rr.setPref3(r3);
            return List.of(rr);
        }

        UUID groupId = UUID.randomUUID();
        List<RoomBookingRequest> out = new ArrayList<>();
        for (List<Room> rooms : grouped.values()) {
            RoomBookingRequest rr = baseRequest(event, requestedBy, groupId);
            rr.setPref1(rooms.size() > 0 ? rooms.get(0) : null);
            rr.setPref2(rooms.size() > 1 ? rooms.get(1) : null);
            rr.setPref3(rooms.size() > 2 ? rooms.get(2) : null);
            out.add(rr);
        }
        return out;
    }

    private static RoomBookingRequest baseRequest(Event event, String requestedBy, UUID splitGroupId) {
        RoomBookingRequest rr = new RoomBookingRequest();
        rr.setEvent(event);
        rr.setStatus(RoomBookingStatus.PENDING);
        rr.setRequestedByUsername(requestedBy);
        rr.setSplitGroupId(splitGroupId);
        return rr;
    }
}
