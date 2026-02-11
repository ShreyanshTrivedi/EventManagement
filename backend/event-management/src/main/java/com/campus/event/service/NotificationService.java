package com.campus.event.service;

import com.campus.event.domain.Notification;
import com.campus.event.domain.NotificationStatus;
import com.campus.event.domain.NotificationType;
import com.campus.event.domain.User;
import com.campus.event.repository.NotificationRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    @Value("${app.notifications.enableEmail:false}")
    private boolean enableEmail;

    @Value("${app.notifications.enableSms:false}")
    private boolean enableSms;

    @Value("${app.notifications.twilio.accountSid:}")
    private String twilioSid;

    @Value("${app.notifications.twilio.authToken:}")
    private String twilioToken;

    @Value("${app.notifications.twilio.fromNumber:}")
    private String twilioFrom;

    public NotificationService(NotificationRepository notificationRepository, JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    public void notifyAllChannels(User user, String subject, String message) {
        // In-app
        saveInApp(user, subject, message, NotificationStatus.SENT);

        // Email
        if (enableEmail && StringUtils.hasText(user.getEmail())) {
            try {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setTo(user.getEmail());
                mail.setSubject(subject);
                mail.setText(message);
                mailSender.send(mail);
                save(NotificationType.EMAIL, user, subject, message, NotificationStatus.SENT);
            } catch (Exception e) {
                log.warn("Email send failed for {}: {}", user.getUsername(), e.getMessage());
                save(NotificationType.EMAIL, user, subject, message, NotificationStatus.FAILED);
            }
        }

        // SMS
        if (enableSms && StringUtils.hasText(user.getPhoneNumber()) && validTwilio()) {
            try {
                Twilio.init(twilioSid, twilioToken);
                Message.creator(new com.twilio.type.PhoneNumber(user.getPhoneNumber()),
                        new com.twilio.type.PhoneNumber(twilioFrom), message).create();
                save(NotificationType.SMS, user, subject, message, NotificationStatus.SENT);
            } catch (Exception e) {
                log.warn("SMS send failed for {}: {}", user.getUsername(), e.getMessage());
                save(NotificationType.SMS, user, subject, message, NotificationStatus.FAILED);
            }
        }
    }

    private boolean validTwilio() {
        return StringUtils.hasText(twilioSid) && StringUtils.hasText(twilioToken) && StringUtils.hasText(twilioFrom)
                && !twilioSid.startsWith("YOUR_") && !twilioFrom.startsWith("+1000000");
    }

    private void saveInApp(User user, String subject, String message, NotificationStatus status) {
        save(NotificationType.IN_APP, user, subject, message, status);
    }

    private void save(NotificationType type, User user, String subject, String message, NotificationStatus status) {
        Notification n = new Notification();
        n.setUser(user);
        n.setType(type);
        n.setSubject(subject);
        n.setMessage(message);
        n.setStatus(status);
        n.setSentAt(status == NotificationStatus.SENT ? LocalDateTime.now() : null);
        notificationRepository.save(n);
    }
}
