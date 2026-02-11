package com.campus.event.repository;

import com.campus.event.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_UsernameOrderByCreatedAtDesc(String username);
}
