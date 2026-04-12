package com.campus.event.web;

import com.campus.event.domain.Resource;
import com.campus.event.domain.ResourceBookingRequest;
import com.campus.event.domain.ResourceType;
import com.campus.event.domain.Role;
import com.campus.event.domain.RoomBookingStatus;
import com.campus.event.domain.User;
import com.campus.event.repository.ResourceBookingRequestRepository;
import com.campus.event.repository.ResourceRepository;
import com.campus.event.repository.UserRepository;
import com.campus.event.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminResourceBookingClaimFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;
    @Autowired private ResourceRepository resourceRepository;
    @Autowired private ResourceBookingRequestRepository requestRepository;

    private String adminToken;
    private User admin;
    private Resource roomResource;
    private Resource openSpace;

    private String generateToken(String username, List<String> roles) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password("dummy")
                .authorities(roles.stream().map(r -> (GrantedAuthority) () -> r).toList())
                .build();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        return jwtTokenService.generateToken(claims, userDetails);
    }

    @BeforeEach
    void setUp() {
        requestRepository.deleteAll();
        userRepository.deleteAll();

        admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
        admin.setEmail("admin@test.com");
        admin.setRoles(Set.of(Role.ADMIN));
        admin = userRepository.save(admin);

        adminToken = generateToken("admin", List.of("ROLE_ADMIN"));

        roomResource = resourceRepository.findAll().stream()
                .filter(r -> r.getResourceType() != ResourceType.OPEN_SPACE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No ROOM-type resource found in test context"));

        // Ensure at least one OPEN_SPACE exists for tests
        openSpace = resourceRepository.findAll().stream()
                .filter(r -> r.getResourceType() == ResourceType.OPEN_SPACE)
                .findFirst()
                .orElse(null);
        if (openSpace == null) {
            Resource r = new Resource();
            r.setName("Test Open Space");
            r.setResourceType(ResourceType.OPEN_SPACE);
            r.setCapacity(100);
            r.setBuilding(roomResource.getBuilding());
            r.setFloor(roomResource.getFloor());
            openSpace = resourceRepository.save(r);
        }

        assertNotNull(openSpace);
    }

    private ResourceBookingRequest createPendingMeetingRequest() {
        ResourceBookingRequest r = new ResourceBookingRequest();
        r.setEvent(null);
        r.setMeetingStart(LocalDateTime.now().plusDays(3));
        r.setMeetingEnd(LocalDateTime.now().plusDays(3).plusHours(1));
        r.setMeetingPurpose("Integration test meeting");
        r.setPref1(roomResource);
        r.setPref2(roomResource);
        r.setPref3(roomResource);
        r.setStatus(RoomBookingStatus.PENDING);
        r.setRequestedByUsername("requester");
        return requestRepository.save(r);
    }

    @Test
    void approveWithoutClaim_returnsForbidden() throws Exception {
        ResourceBookingRequest req = createPendingMeetingRequest();

        mockMvc.perform(post("/api/admin/room-requests/" + req.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"allocatedResourceId\":" + roomResource.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("active claim")));
    }

    @Test
    void claimThenApprove_succeeds() throws Exception {
        ResourceBookingRequest req = createPendingMeetingRequest();

        mockMvc.perform(post("/api/admin/room-requests/" + req.getId() + "/claim")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/room-requests/" + req.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"allocatedResourceId\":" + roomResource.getId() + "}"))
                .andExpect(status().isOk());

        ResourceBookingRequest updated = requestRepository.findById(req.getId()).orElseThrow();
        assertEquals(RoomBookingStatus.APPROVED, updated.getStatus());
    }

    @Test
    void expiredClaim_preventsApprove() throws Exception {
        ResourceBookingRequest req = createPendingMeetingRequest();
        req.setClaimedBy(admin);
        req.setClaimedAt(LocalDateTime.now().minusMinutes(16));
        requestRepository.save(req);

        mockMvc.perform(post("/api/admin/room-requests/" + req.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"allocatedResourceId\":" + roomResource.getId() + "}"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("active claim")));
    }

    @Test
    void meetingAllocation_openSpaceRejected() throws Exception {
        ResourceBookingRequest req = createPendingMeetingRequest();

        mockMvc.perform(post("/api/admin/room-requests/" + req.getId() + "/claim")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/room-requests/" + req.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType("application/json")
                        .content("{\"allocatedResourceId\":" + openSpace.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Meetings can only")));
    }
}
