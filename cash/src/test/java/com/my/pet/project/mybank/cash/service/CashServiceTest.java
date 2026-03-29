package com.my.pet.project.mybank.cash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.pet.project.mybank.cash.client.AccountClient;
import com.my.pet.project.mybank.cash.dto.AccountResponse;
import com.my.pet.project.mybank.cash.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.cash.dto.CashRequest;
import com.my.pet.project.mybank.cash.dto.CashResponse;
import com.my.pet.project.mybank.cash.exception.InsufficientFundsException;
import com.my.pet.project.mybank.cash.model.CashOperation;
import com.my.pet.project.mybank.cash.model.CashOperationType;
import com.my.pet.project.mybank.cash.model.OutboxEvent;
import com.my.pet.project.mybank.cash.repository.CashOperationRepository;
import com.my.pet.project.mybank.cash.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashServiceTest {

    @Mock
    private AccountClient accountClient;

    @Mock
    private CashOperationRepository cashOperationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CashService cashService;

    private AccountResponse accountWithBalance(BigDecimal balance) {
        return new AccountResponse(1L, "user1", "John", "Doe", null, balance, 0L);
    }

    @Test
    void processCash_deposit_success() {
        when(accountClient.getAccountById(1L)).thenReturn(accountWithBalance(BigDecimal.valueOf(500)));
        when(accountClient.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(accountWithBalance(BigDecimal.valueOf(600)));

        CashResponse response = cashService.processCash(new CashRequest(1L, new BigDecimal("100"), "PUT"));

        assertThat(response.newBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(response.message()).contains("100");

        ArgumentCaptor<CashOperation> operationCaptor = ArgumentCaptor.forClass(CashOperation.class);
        verify(cashOperationRepository).save(operationCaptor.capture());
        assertThat(operationCaptor.getValue().getType()).isEqualTo(CashOperationType.DEPOSIT);
        assertThat(operationCaptor.getValue().getAccountId()).isEqualTo(1L);
        assertThat(operationCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CASH_DEPOSIT");
    }

    @Test
    void processCash_withdrawal_success() {
        when(accountClient.getAccountById(1L)).thenReturn(accountWithBalance(BigDecimal.valueOf(500)));
        when(accountClient.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(accountWithBalance(BigDecimal.valueOf(450)));

        CashResponse response = cashService.processCash(new CashRequest(1L, new BigDecimal("50"), "GET"));

        assertThat(response.newBalance()).isEqualByComparingTo(BigDecimal.valueOf(450));

        ArgumentCaptor<CashOperation> operationCaptor = ArgumentCaptor.forClass(CashOperation.class);
        verify(cashOperationRepository).save(operationCaptor.capture());
        assertThat(operationCaptor.getValue().getType()).isEqualTo(CashOperationType.WITHDRAWAL);

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CASH_WITHDRAWAL");
    }

    @Test
    void processCash_withdrawal_insufficientFunds() {
        when(accountClient.getAccountById(1L)).thenReturn(accountWithBalance(BigDecimal.valueOf(500)));

        assertThatThrownBy(() -> cashService.processCash(new CashRequest(1L, new BigDecimal("600"), "GET")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountClient, never()).updateBalance(any(), any());
        verify(cashOperationRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }
}
