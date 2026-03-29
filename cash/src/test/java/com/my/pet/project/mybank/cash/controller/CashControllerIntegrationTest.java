package com.my.pet.project.mybank.cash.controller;

import com.my.pet.project.mybank.cash.client.AccountClient;
import com.my.pet.project.mybank.cash.dto.AccountResponse;
import com.my.pet.project.mybank.cash.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.cash.dto.CashRequest;
import com.my.pet.project.mybank.cash.service.OutboxPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    @Autowired
    private ObjectMapper objectMapper;

    private AccountResponse accountWithBalance(BigDecimal balance) {
        return new AccountResponse(1L, "user1", "John", "Doe", null, balance, 0L);
    }

    @Test
    void processCash_deposit_returns200() throws Exception {
        when(accountClient.getAccountById(1L)).thenReturn(accountWithBalance(BigDecimal.valueOf(500)));
        when(accountClient.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(accountWithBalance(BigDecimal.valueOf(600)));

        mockMvc.perform(post("/cash")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CashRequest(1L, new BigDecimal("100"), "PUT"))))
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
                        .content(objectMapper.writeValueAsString(new CashRequest(1L, new BigDecimal("200"), "GET"))))
                .andExpect(status().isBadRequest());
    }
}
