package com.my.pet.project.mybank.transfer.service;

import com.my.pet.project.mybank.transfer.model.OutboxEvent;
import com.my.pet.project.mybank.transfer.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
    @SchedulerLock(name = "publishOutboxEvents_transfer", lockAtMostFor = "4s")
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findBySentFalse();
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send("notifications.transfer", String.valueOf(event.getId()), event.getPayload()).get();

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
