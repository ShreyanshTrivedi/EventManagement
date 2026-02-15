package com.campus.event.repository;

import com.campus.event.domain.NotificationThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationThreadRepository extends JpaRepository<NotificationThread, Long> {
    List<NotificationThread> findByEvent_IdOrderByCreatedAtDesc(Long eventId);
}
