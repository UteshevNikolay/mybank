package com.my.pet.project.mybank.cash.service;

import com.my.pet.project.mybank.cash.model.OutboxEvent;
import com.my.pet.project.mybank.cash.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findBySentFalse();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send("notifications", String.valueOf(event.getId()), event.getPayload()).get();

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
