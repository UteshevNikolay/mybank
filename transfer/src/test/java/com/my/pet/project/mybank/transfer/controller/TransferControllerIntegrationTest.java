package com.my.pet.project.mybank.transfer.controller;

import com.my.pet.project.mybank.transfer.client.AccountClient;
import com.my.pet.project.mybank.transfer.dto.AccountResponse;
import com.my.pet.project.mybank.transfer.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.transfer.dto.TransferRequest;
import com.my.pet.project.mybank.transfer.service.OutboxPublisher;
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

    @Autowired
    private ObjectMapper objectMapper;

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
                        .content(objectMapper.writeValueAsString(new TransferRequest(1L, "user2", 100))))
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
                        .content(objectMapper.writeValueAsString(new TransferRequest(1L, "user2", 100))))
                .andExpect(status().isBadRequest());
    }
}
