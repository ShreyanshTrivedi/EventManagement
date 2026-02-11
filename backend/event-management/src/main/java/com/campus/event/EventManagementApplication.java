package com.campus.event;

import com.campus.event.domain.Room;
import com.campus.event.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventManagementApplication.class, args);
    }

    @Bean
    CommandLineRunner seedRooms(RoomRepository rooms) {
        return args -> {
            if (rooms.count() == 0) {
                Room a = new Room(); a.setName("Conference Room A"); a.setLocation("Building A, Floor 2"); a.setCapacity(20);
                Room b = new Room(); b.setName("Lecture Hall B"); b.setLocation("Building B, Floor 1"); b.setCapacity(50);
                Room c = new Room(); c.setName("Meeting Room C"); c.setLocation("Building C, Floor 3"); c.setCapacity(10);
                Room d = new Room(); d.setName("Study Room D"); d.setLocation("Library, Floor 1"); d.setCapacity(8);
                Room e = new Room(); e.setName("Seminar Room E"); e.setLocation("Building A, Floor 3"); e.setCapacity(30);
                rooms.save(a); rooms.save(b); rooms.save(c); rooms.save(d); rooms.save(e);
            }
        };
    }
}


