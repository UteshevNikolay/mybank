package com.my.pet.project.mybank.notifications.service;

import com.my.pet.project.mybank.notifications.dto.NotificationRequest;
import com.my.pet.project.mybank.notifications.model.Notification;
import com.my.pet.project.mybank.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void processNotification(NotificationRequest request) {
        Notification notification = new Notification();
        notification.setAccountId(request.accountId());
        notification.setEventType(request.eventType());
        notification.setMessage(request.message());
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);

        log.info("Notification saved: accountId={}, type={}, message={}",
                request.accountId(), request.eventType(), request.message());
    }
}
