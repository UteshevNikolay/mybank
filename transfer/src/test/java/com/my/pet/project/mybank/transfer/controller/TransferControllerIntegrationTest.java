package com.my.pet.project.mybank.transfer.controller;

import com.my.pet.project.mybank.transfer.client.AccountClient;
import com.my.pet.project.mybank.transfer.dto.AccountResponse;
import com.my.pet.project.mybank.transfer.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.transfer.dto.TransferRequest;
import com.my.pet.project.mybank.transfer.service.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class TransferControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountClient accountClient;

    @MockitoBean
    private OutboxPublisher outboxPublisher;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @TestConfiguration(proxyBeanMethods = false)
    static class TestConfig {

        @Bean
        ClientRegistrationRepository clientRegistrationRepository() {
            ClientRegistration registration = ClientRegistration.withRegistrationId("transfer-service")
                    .clientId("test-client")
                    .clientSecret("test-secret")
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .tokenUri("http://localhost:9999/token")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }

        @Bean
        RestClient accountsRestClient() {
            return RestClient.builder().baseUrl("http://localhost:9999").build();
        }

        @Bean
        RestClient notificationsRestClient() {
            return RestClient.builder().baseUrl("http://localhost:9999").build();
        }
    }

    private AccountResponse senderAccount(BigDecimal balance) {
        return new AccountResponse(1L, "sender", "Ivan", "Ivanov", null, balance);
    }

    private AccountResponse recipientAccount(BigDecimal balance) {
        return new AccountResponse(2L, "user2", "Petr", "Petrov", null, balance);
    }

    @Test
    void processTransfer_returns200() throws Exception {
        when(accountClient.getAccountById(1L)).thenReturn(senderAccount(BigDecimal.valueOf(500)));
        when(accountClient.getAccountByLogin("user2")).thenReturn(recipientAccount(BigDecimal.valueOf(200)));
        when(accountClient.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(senderAccount(BigDecimal.valueOf(400)));
        when(accountClient.updateBalance(eq(2L), any(BalanceUpdateRequest.class)))
                .thenReturn(recipientAccount(BigDecimal.valueOf(300)));

        mockMvc.perform(post("/transfer")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new TransferRequest(1L, "user2", 100))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newBalance").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void processTransfer_insufficientFunds_returns400() throws Exception {
        when(accountClient.getAccountById(1L)).thenReturn(senderAccount(BigDecimal.valueOf(50)));

        mockMvc.perform(post("/transfer")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new TransferRequest(1L, "user2", 100))))
                .andExpect(status().isBadRequest());
    }
}
