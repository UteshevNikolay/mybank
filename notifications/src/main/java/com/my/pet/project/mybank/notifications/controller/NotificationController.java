package com.my.pet.project.mybank.notifications.controller;

import com.my.pet.project.mybank.notifications.dto.NotificationRequest;
import com.my.pet.project.mybank.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Void> receiveNotification(@RequestBody NotificationRequest request) {
        notificationService.processNotification(request);
        return ResponseEntity.ok().build();
    }
}
