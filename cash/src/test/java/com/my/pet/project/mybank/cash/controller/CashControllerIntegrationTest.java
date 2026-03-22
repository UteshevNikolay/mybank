package com.my.pet.project.mybank.cash.controller;

import com.my.pet.project.mybank.cash.client.AccountClient;
import com.my.pet.project.mybank.cash.dto.AccountResponse;
import com.my.pet.project.mybank.cash.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.cash.dto.CashRequest;
import com.my.pet.project.mybank.cash.service.OutboxPublisher;
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
class CashControllerIntegrationTest {

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
            ClientRegistration registration = ClientRegistration.withRegistrationId("cash-service")
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

    private AccountResponse accountWithBalance(BigDecimal balance) {
        return new AccountResponse(1L, "user1", "John", "Doe", null, balance);
    }

    @Test
    void processCash_deposit_returns200() throws Exception {
        when(accountClient.getAccountById(1L)).thenReturn(accountWithBalance(BigDecimal.valueOf(500)));
        when(accountClient.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(accountWithBalance(BigDecimal.valueOf(600)));

        mockMvc.perform(post("/cash")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CashRequest(1L, 100, "PUT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.newBalance").value(600));
    }

    @Test
    void processCash_withdrawal_insufficientFunds_returns400() throws Exception {
        when(accountClient.getAccountById(1L)).thenReturn(accountWithBalance(BigDecimal.valueOf(100)));

        mockMvc.perform(post("/cash")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(new CashRequest(1L, 200, "GET"))))
                .andExpect(status().isBadRequest());
    }
}
