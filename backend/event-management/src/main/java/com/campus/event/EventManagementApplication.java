package com.campus.event;

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
            // Room initialization is now handled by DataInitializationService
            // This method is kept for compatibility but does nothing
            System.out.println("Room initialization handled by DataInitializationService");
        };
    }
}
