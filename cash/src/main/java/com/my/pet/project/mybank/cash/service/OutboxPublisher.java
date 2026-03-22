package com.my.pet.project.mybank.cash.service;

import com.my.pet.project.mybank.cash.model.OutboxEvent;
import com.my.pet.project.mybank.cash.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RestClient notificationsRestClient;

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findBySentFalse();
        for (OutboxEvent event : events) {
            try {
                notificationsRestClient.post()
                        .uri("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(event.getPayload())
                        .retrieve()
                        .toBodilessEntity();

                event.setSent(true);
                outboxEventRepository.save(event);
                log.info("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.warn("Failed to publish outbox event: id={}, type={}, error={}",
                        event.getId(), event.getEventType(), e.getMessage());
            }
        }
    }
}
