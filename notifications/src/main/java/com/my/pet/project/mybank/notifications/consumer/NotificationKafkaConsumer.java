package com.my.pet.project.mybank.notifications.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.pet.project.mybank.notifications.dto.NotificationRequest;
import com.my.pet.project.mybank.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"notifications.accounts", "notifications.cash", "notifications.transfer"}, groupId = "notifications-group")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            NotificationRequest request = objectMapper.readValue(record.value(), NotificationRequest.class);
            notificationService.processNotification(request);
            acknowledgment.acknowledge();
            log.info("Processed notification: key={}, type={}", record.key(), request.eventType());
        } catch (Exception e) {
            log.error("Failed to process notification: key={}, error={}", record.key(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
