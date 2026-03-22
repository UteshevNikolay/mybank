package com.my.pet.project.mybank.notifications.dto;

public record NotificationRequest(
        Long accountId,
        String eventType,
        String message
) {}
