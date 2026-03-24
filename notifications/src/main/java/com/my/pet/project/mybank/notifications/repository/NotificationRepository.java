package com.my.pet.project.mybank.notifications.repository;

import com.my.pet.project.mybank.notifications.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
