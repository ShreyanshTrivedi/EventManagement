package com.campus.event.repository;

import com.campus.event.domain.ThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThreadMessageRepository extends JpaRepository<ThreadMessage, Long> {
    List<ThreadMessage> findByThread_IdOrderByCreatedAtAsc(Long threadId);
}
