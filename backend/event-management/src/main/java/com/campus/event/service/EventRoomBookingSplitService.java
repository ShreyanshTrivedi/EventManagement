package com.campus.event.service;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Event;
import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
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
    public List<ResourceBookingRequest> buildEventRequests(Event event, Resource r1, Resource r2, Resource r3, String requestedBy) {
        List<Resource> prefs = List.of(r1, r2, r3);
        Map<AdminScope, List<Resource>> grouped = new LinkedHashMap<>();
        for (Resource r : prefs) {
            AdminScope s = RoomApprovalRules.scopeForResource(r);
            grouped.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
        }

        if (grouped.size() == 1) {
            ResourceBookingRequest rr = baseRequest(event, requestedBy, null);
            rr.setPref1(r1);
            rr.setPref2(r2);
            rr.setPref3(r3);
            return List.of(rr);
        }

        UUID groupId = UUID.randomUUID();
        List<ResourceBookingRequest> out = new ArrayList<>();
        for (List<Resource> resources : grouped.values()) {
            ResourceBookingRequest rr = baseRequest(event, requestedBy, groupId);
            rr.setPref1(resources.size() > 0 ? resources.get(0) : null);
            rr.setPref2(resources.size() > 1 ? resources.get(1) : null);
            rr.setPref3(resources.size() > 2 ? resources.get(2) : null);
            out.add(rr);
        }
        return out;
    }

    private static ResourceBookingRequest baseRequest(Event event, String requestedBy, UUID splitGroupId) {
        ResourceBookingRequest rr = new ResourceBookingRequest();
        rr.setEvent(event);
        rr.setStatus(RoomBookingStatus.PENDING);
        rr.setRequestedByUsername(requestedBy);
        rr.setSplitGroupId(splitGroupId);
        return rr;
    }
}
