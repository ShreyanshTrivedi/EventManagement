package com.campus.event.config;

import com.campus.event.domain.AdminScope;
import com.campus.event.domain.Building;
import com.campus.event.domain.Event;
import com.campus.event.domain.Role;
import com.campus.event.domain.User;
import com.campus.event.repository.BuildingRepository;
import com.campus.event.repository.EventRepository;
import com.campus.event.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Set;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seedData(UserRepository users, EventRepository events,
                               BuildingRepository buildings, PasswordEncoder encoder) {
        return args -> {
            // Migrate any existing users with non-BCrypt passwords to encoded values
            users.findAll().forEach(u -> {
                String ph = u.getPasswordHash();
                if (ph != null && !ph.startsWith("$2")) {
                    u.setPasswordHash(encoder.encode(ph));
                    users.save(u);
                    log.info("Migrated password hash for user: {}", u.getUsername());
                }
            });

            if (users.findByUsername("central_admin").isEmpty()) {
                User centralAdmin = new User();
                centralAdmin.setUsername("central_admin");
                centralAdmin.setPasswordHash(encoder.encode("Central@123"));
                centralAdmin.setEmail("central@example.com");
                centralAdmin.setRoles(Set.of(Role.CENTRAL_ADMIN));
                users.save(centralAdmin);
                log.info("Seeded central admin user");
            }

            syncBuildingAdmin(buildings, users, "ab1_admin", "BLD_A", AdminScope.LARGE_HALL);
            syncBuildingAdmin(buildings, users, "ab2_admin", "BLD_B", AdminScope.LARGE_HALL);

            if (users.findByUsername("ab1_admin").isEmpty()) {
                User ab1Admin = new User();
                ab1Admin.setUsername("ab1_admin");
                ab1Admin.setPasswordHash(encoder.encode("Admin@AB1"));
                ab1Admin.setEmail("ab1@example.com");
                ab1Admin.setRoles(Set.of(Role.BUILDING_ADMIN));
                ab1Admin.setAdminScope(AdminScope.LARGE_HALL);
                buildings.findByCode("BLD_A").ifPresentOrElse(
                        b -> ab1Admin.setManagedBuildingId(b.getId()),
                        () -> ab1Admin.setManagedBuildingId(1L));
                users.save(ab1Admin);
                log.info("Seeded AB1 building admin (large hall)");
            }

            if (users.findByUsername("ab2_admin").isEmpty()) {
                User ab2Admin = new User();
                ab2Admin.setUsername("ab2_admin");
                ab2Admin.setPasswordHash(encoder.encode("Admin@AB2"));
                ab2Admin.setEmail("ab2@example.com");
                ab2Admin.setRoles(Set.of(Role.BUILDING_ADMIN));
                ab2Admin.setAdminScope(AdminScope.LARGE_HALL);
                buildings.findByCode("BLD_B").ifPresentOrElse(
                        b -> ab2Admin.setManagedBuildingId(b.getId()),
                        () -> ab2Admin.setManagedBuildingId(2L));
                users.save(ab2Admin);
                log.info("Seeded AB2 building admin (large hall)");
            }

            seedBuildingRoomAdminIfAbsent(users, buildings, encoder, "building_a_room", "RoomA@123",
                    "bld-a-room@example.com", "BLD_A", AdminScope.NORMAL_ROOM);
            seedBuildingRoomAdminIfAbsent(users, buildings, encoder, "building_b_room", "RoomB@123",
                    "bld-b-room@example.com", "BLD_B", AdminScope.NORMAL_ROOM);

            if (users.findByUsername("faculty").isEmpty()) {
                User faculty = new User();
                faculty.setUsername("faculty");
                faculty.setPasswordHash(encoder.encode("Faculty@123"));
                faculty.setEmail("faculty@example.com");
                faculty.setRoles(Set.of(Role.FACULTY));
                users.save(faculty);
                log.info("Seeded faculty user");
            }

            if (users.findByUsername("club").isEmpty()) {
                User club = new User();
                club.setUsername("club");
                club.setPasswordHash(encoder.encode("Club@123"));
                club.setEmail("club@example.com");
                club.setRoles(Set.of(Role.CLUB_ASSOCIATE));
                users.save(club);
                log.info("Seeded club user");
            }

            if (users.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setPasswordHash(encoder.encode("User@123"));
                user.setEmail("user@example.com");
                user.setRoles(Set.of(Role.GENERAL_USER));
                users.save(user);
                log.info("Seeded general user");
            }

            if (events.count() == 0) {
                // Find the first active building to assign to sample events
                Building defaultBuilding = buildings.findByIsActiveTrue().stream()
                        .findFirst()
                        .orElseGet(() -> {
                            log.warn("No active buildings found. Creating a default building for seed events.");
                            Building b = new Building("Main Building", "MAIN", "Default building for seed data");
                            return buildings.save(b);
                        });

                Event e1 = new Event();
                e1.setTitle("Orientation Week");
                e1.setDescription("Welcome event for new students");
                e1.setStartTime(LocalDateTime.now().plusDays(3).withHour(10).withMinute(0));
                e1.setEndTime(LocalDateTime.now().plusDays(3).withHour(12).withMinute(0));
                e1.setPublic(true);
                e1.setBuilding(defaultBuilding);
                events.save(e1);

                Event e2 = new Event();
                e2.setTitle("Tech Talk: Spring Boot Security");
                e2.setDescription("Deep dive into JWT and RBAC");
                e2.setStartTime(LocalDateTime.now().plusDays(7).withHour(15).withMinute(0));
                e2.setEndTime(LocalDateTime.now().plusDays(7).withHour(17).withMinute(0));
                e2.setPublic(true);
                e2.setBuilding(defaultBuilding);
                events.save(e2);

                log.info("Seeded sample events assigned to building: {}", defaultBuilding.getName());
            }
        };
    }

    private static void syncBuildingAdmin(BuildingRepository buildings, UserRepository users,
                                          String username, String buildingCode, AdminScope scope) {
        users.findByUsername(username).ifPresent(u -> {
            if (!u.getRoles().contains(Role.BUILDING_ADMIN)) {
                return;
            }
            buildings.findByCode(buildingCode).ifPresent(b -> {
                boolean changed = false;
                if (u.getManagedBuildingId() == null || !u.getManagedBuildingId().equals(b.getId())) {
                    u.setManagedBuildingId(b.getId());
                    changed = true;
                }
                if (u.getAdminScope() != scope) {
                    u.setAdminScope(scope);
                    changed = true;
                }
                if (changed) {
                    users.save(u);
                }
            });
        });
    }

    private static void seedBuildingRoomAdminIfAbsent(UserRepository users, BuildingRepository buildings,
                                                      PasswordEncoder encoder, String username, String rawPassword,
                                                      String email, String buildingCode, AdminScope scope) {
        if (users.findByUsername(username).isPresent()) {
            return;
        }
        buildings.findByCode(buildingCode).ifPresent(b -> {
            User u = new User();
            u.setUsername(username);
            u.setPasswordHash(encoder.encode(rawPassword));
            u.setEmail(email);
            u.setRoles(Set.of(Role.BUILDING_ADMIN));
            u.setManagedBuildingId(b.getId());
            u.setAdminScope(scope);
            users.save(u);
        });
    }
}
