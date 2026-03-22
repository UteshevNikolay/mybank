package com.my.pet.project.mybank.transfer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.pet.project.mybank.transfer.client.AccountClient;
import com.my.pet.project.mybank.transfer.dto.AccountResponse;
import com.my.pet.project.mybank.transfer.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.transfer.dto.TransferRequest;
import com.my.pet.project.mybank.transfer.dto.TransferResponse;
import com.my.pet.project.mybank.transfer.exception.InsufficientFundsException;
import com.my.pet.project.mybank.transfer.model.OutboxEvent;
import com.my.pet.project.mybank.transfer.model.TransferOperation;
import com.my.pet.project.mybank.transfer.repository.OutboxEventRepository;
import com.my.pet.project.mybank.transfer.repository.TransferOperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransferOperationRepository transferOperationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private TransferService transferService;

    private AccountResponse senderAccount(BigDecimal balance) {
        return new AccountResponse(1L, "sender", "Ivan", "Ivanov", null, balance);
    }

    private AccountResponse recipientAccount(BigDecimal balance) {
        return new AccountResponse(2L, "recipient", "Petr", "Petrov", null, balance);
    }

    @Test
    void processTransfer_success() {
        when(accountClient.getAccountById(1L)).thenReturn(senderAccount(BigDecimal.valueOf(500)));
        when(accountClient.getAccountByLogin("user2")).thenReturn(recipientAccount(BigDecimal.valueOf(200)));
        when(accountClient.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(senderAccount(BigDecimal.valueOf(400)));
        when(accountClient.updateBalance(eq(2L), any(BalanceUpdateRequest.class)))
                .thenReturn(recipientAccount(BigDecimal.valueOf(300)));

        TransferResponse response = transferService.processTransfer(new TransferRequest(1L, "user2", new BigDecimal("100")));

        assertThat(response.newBalance()).isEqualByComparingTo(BigDecimal.valueOf(400));

        ArgumentCaptor<TransferOperation> operationCaptor = ArgumentCaptor.forClass(TransferOperation.class);
        verify(transferOperationRepository).save(operationCaptor.capture());
        assertThat(operationCaptor.getValue().getFromAccountId()).isEqualTo(1L);
        assertThat(operationCaptor.getValue().getToAccountId()).isEqualTo(2L);
        assertThat(operationCaptor.getValue().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(2)).save(eventCaptor.capture());
        List<OutboxEvent> events = eventCaptor.getAllValues();
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("TRANSFER_SENT", "TRANSFER_RECEIVED");
    }

    @Test
    void processTransfer_insufficientFunds() {
        when(accountClient.getAccountById(1L)).thenReturn(senderAccount(BigDecimal.valueOf(50)));

        assertThatThrownBy(() -> transferService.processTransfer(new TransferRequest(1L, "user2", new BigDecimal("100"))))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountClient, never()).updateBalance(any(), any());
        verify(transferOperationRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }
}
