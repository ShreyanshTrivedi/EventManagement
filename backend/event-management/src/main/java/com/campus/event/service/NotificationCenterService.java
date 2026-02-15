package com.campus.event.service;

import com.campus.event.domain.*;
import com.campus.event.repository.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationCenterService {
    private final NotificationMessageRepository messageRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationThreadRepository threadRepository;
    private final ThreadMessageRepository threadMessageRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public NotificationCenterService(NotificationMessageRepository messageRepository,
                                     NotificationDeliveryRepository deliveryRepository,
                                     NotificationThreadRepository threadRepository,
                                     ThreadMessageRepository threadMessageRepository,
                                     EventRegistrationRepository registrationRepository,
                                     UserRepository userRepository,
                                     EventRepository eventRepository) {
        this.messageRepository = messageRepository;
        this.deliveryRepository = deliveryRepository;
        this.threadRepository = threadRepository;
        this.threadMessageRepository = threadMessageRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    public List<NotificationDelivery> getDeliveriesForUser(String username) {
        return deliveryRepository.findByUser_UsernameOrderByCreatedAtDesc(username);
    }

    @Transactional
    public NotificationMessage createBroadcast(String title, String message, Urgency urgency, boolean threadEnabled, User createdBy) {
        NotificationMessage nm = new NotificationMessage();
        nm.setTitle(title);
        nm.setMessage(message);
        nm.setOrigin(NotificationOrigin.GLOBAL);
        nm.setUrgency(urgency != null ? urgency : Urgency.NORMAL);
        nm.setThreadEnabled(threadEnabled);
        nm.setCreatedBy(createdBy);
        nm = messageRepository.save(nm);

        // fan-out to all users (synchronous for MVP)
        List<User> users = userRepository.findAll();
        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (User u : users) {
            NotificationDelivery d = new NotificationDelivery();
            d.setNotification(nm);
            d.setUser(u);
            d.setDeliveryStatus(NotificationStatus.PENDING);
            deliveries.add(d);
        }
        deliveryRepository.saveAll(deliveries);
        return nm;
    }

    @Transactional
    public NotificationMessage createEventNotification(Long eventId, String title, String message, Urgency urgency, boolean threadEnabled, User createdBy) {
        Event ev = eventRepository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));
        NotificationMessage nm = new NotificationMessage();
        nm.setTitle(title);
        nm.setMessage(message);
        nm.setOrigin(NotificationOrigin.EVENT);
        nm.setEvent(ev);
        nm.setUrgency(urgency != null ? urgency : Urgency.NORMAL);
        nm.setThreadEnabled(threadEnabled);
        nm.setCreatedBy(createdBy);
        nm = messageRepository.save(nm);

        List<String> usernames = registrationRepository.findUsernamesByEventId(eventId);
        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (String username : usernames) {
            User u = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException(username));
            NotificationDelivery d = new NotificationDelivery();
            d.setNotification(nm);
            d.setUser(u);
            d.setDeliveryStatus(NotificationStatus.PENDING);
            deliveries.add(d);
        }
        deliveryRepository.saveAll(deliveries);
        return nm;
    }

    @Transactional
    public NotificationDelivery markDeliveryRead(Long deliveryId, String username) {
        Optional<NotificationDelivery> od = deliveryRepository.findById(deliveryId);
        if (od.isEmpty()) throw new IllegalArgumentException("Delivery not found");
        NotificationDelivery d = od.get();
        if (!d.getUser().getUsername().equals(username)) throw new SecurityException("Not allowed");
        d.setReadAt(LocalDateTime.now());
        return deliveryRepository.save(d);
    }

    @Transactional
    public NotificationDelivery muteDelivery(Long deliveryId, String username, boolean mute) {
        NotificationDelivery d = deliveryRepository.findById(deliveryId).orElseThrow(() -> new IllegalArgumentException("Delivery not found"));
        if (!d.getUser().getUsername().equals(username)) throw new SecurityException("Not allowed");
        d.setMuted(mute);
        return deliveryRepository.save(d);
    }

    public List<NotificationThread> getThreadsForEvent(Long eventId) {
        return threadRepository.findByEvent_IdOrderByCreatedAtDesc(eventId);
    }

    @Transactional
    public NotificationThread createThreadForNotification(Long notificationId, String title, String message, User author) {
        NotificationMessage nm = messageRepository.findById(notificationId).orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        NotificationThread t = new NotificationThread();
        t.setNotification(nm);
        t.setEvent(nm.getEvent());
        t.setTitle(title != null ? title : (nm.getTitle() != null ? nm.getTitle() : "Discussion"));
        t.setCreatedBy(author);
        t = threadRepository.save(t);
        if (message != null && !message.isBlank()) {
            ThreadMessage tm = new ThreadMessage();
            tm.setThread(t);
            tm.setAuthor(author);
            tm.setContent(message);
            threadMessageRepository.save(tm);
        }
        return t;
    }

    @Transactional
    public NotificationThread createThreadForEvent(Long eventId, String title, String message, User author) {
        Event ev = eventRepository.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Event not found"));
        NotificationThread t = new NotificationThread();
        t.setEvent(ev);
        t.setTitle(title != null ? title : (ev.getTitle() != null ? ev.getTitle() + " - Discussion" : "Discussion"));
        t.setCreatedBy(author);
        t = threadRepository.save(t);
        if (message != null && !message.isBlank()) {
            ThreadMessage tm = new ThreadMessage();
            tm.setThread(t);
            tm.setAuthor(author);
            tm.setContent(message);
            threadMessageRepository.save(tm);
        }
        return t;
    }

    public List<ThreadMessage> getMessages(Long threadId) {
        return threadMessageRepository.findByThread_IdOrderByCreatedAtAsc(threadId);
    }

    @Transactional
    public ThreadMessage postThreadMessage(Long threadId, User author, String content) {
        NotificationThread t = threadRepository.findById(threadId).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        ThreadMessage m = new ThreadMessage();
        m.setThread(t);
        m.setAuthor(author);
        m.setContent(content);
        return threadMessageRepository.save(m);
    }

    @Transactional
    public NotificationThread closeThread(Long threadId, User actor) {
        NotificationThread t = threadRepository.findById(threadId).orElseThrow(() -> new IllegalArgumentException("Thread not found"));
        // only creator or admin allowed — controller enforces policy, here we simply set
        t.setClosed(true);
        return threadRepository.save(t);
    }
}
