package com.my.pet.project.mybank.notifications.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.pet.project.mybank.notifications.dto.NotificationRequest;
import com.my.pet.project.mybank.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class NotificationKafkaIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.6.1");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void consumeNotification_savesToDatabase() throws Exception {
        NotificationRequest request = new NotificationRequest(10L, "CASH_DEPOSIT", "Deposited 500");
        String payload = objectMapper.writeValueAsString(request);

        kafkaTemplate.send("notifications.cash", "1", payload).get();

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(notificationRepository.findAll()).hasSize(1);
                    assertThat(notificationRepository.findAll().get(0).getAccountId()).isEqualTo(10L);
                    assertThat(notificationRepository.findAll().get(0).getEventType()).isEqualTo("CASH_DEPOSIT");
                    assertThat(notificationRepository.findAll().get(0).getMessage()).isEqualTo("Deposited 500");
                });
    }
}
