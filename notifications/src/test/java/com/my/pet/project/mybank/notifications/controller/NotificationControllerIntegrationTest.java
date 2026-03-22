package com.my.pet.project.mybank.notifications.controller;

import com.my.pet.project.mybank.notifications.dto.NotificationRequest;
import com.my.pet.project.mybank.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.consul.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.loadbalancer.enabled=false",
        "spring.cloud.compatibility-verifier.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9999/not-used",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.autoconfigure.exclude=" +
                "org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration," +
                "org.springframework.cloud.autoconfigure.RefreshAutoConfiguration," +
                "org.springframework.cloud.autoconfigure.ConfigurationPropertiesRebinderAutoConfiguration," +
                "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration," +
                "org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClientAutoConfiguration"
})
@AutoConfigureMockMvc
@Testcontainers
class NotificationControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void processNotification_returns200() throws Exception {
        NotificationRequest request = new NotificationRequest(10L, "DEPOSIT", "Deposited 500");

        mockMvc.perform(post("/notifications")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SERVICE_ACCESS")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(notificationRepository.findAll().get(0).getAccountId()).isEqualTo(10L);
        assertThat(notificationRepository.findAll().get(0).getEventType()).isEqualTo("DEPOSIT");
        assertThat(notificationRepository.findAll().get(0).getMessage()).isEqualTo("Deposited 500");
    }
}
