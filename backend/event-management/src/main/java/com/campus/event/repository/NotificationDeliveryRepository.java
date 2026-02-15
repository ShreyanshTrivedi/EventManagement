package com.campus.event.repository;

import com.campus.event.domain.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
    List<NotificationDelivery> findByUser_UsernameOrderByCreatedAtDesc(String username);
    List<NotificationDelivery> findByNotification_Event_IdAndUser_UsernameOrderByCreatedAtDesc(Long eventId, String username);
}
