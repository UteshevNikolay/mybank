package com.my.pet.project.mybank.transfer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountClient accountClient;
    private final TransferOperationRepository transferOperationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        AccountResponse sender = accountClient.getAccountById(request.fromAccountId());
        BigDecimal amount = request.value();

        if (sender.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на счету");
        }

        AccountResponse recipient = accountClient.getAccountByLogin(request.toLogin());

        BigDecimal newSenderBalance = sender.balance().subtract(amount);
        accountClient.updateBalance(sender.id(), new BalanceUpdateRequest(newSenderBalance, sender.version()));

        BigDecimal newRecipientBalance = recipient.balance().add(amount);
        accountClient.updateBalance(recipient.id(), new BalanceUpdateRequest(newRecipientBalance, recipient.version()));

        TransferOperation operation = new TransferOperation();
        operation.setFromAccountId(sender.id());
        operation.setToAccountId(recipient.id());
        operation.setAmount(amount);
        operation.setCreatedAt(LocalDateTime.now());
        transferOperationRepository.save(operation);

        String recipientName = recipient.lastName() + " " + recipient.firstName();

        OutboxEvent senderEvent = new OutboxEvent();
        senderEvent.setEventType("TRANSFER_SENT");
        senderEvent.setPayload(toJson(Map.of(
                "accountId", sender.id(),
                "eventType", "TRANSFER_SENT",
                "message", "Перевод %s руб клиенту %s".formatted(amount.toPlainString(), recipientName))));
        senderEvent.setSent(false);
        senderEvent.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(senderEvent);

        OutboxEvent recipientEvent = new OutboxEvent();
        recipientEvent.setEventType("TRANSFER_RECEIVED");
        recipientEvent.setPayload(toJson(Map.of(
                "accountId", recipient.id(),
                "eventType", "TRANSFER_RECEIVED",
                "message", "Получен перевод %s руб".formatted(amount.toPlainString()))));
        recipientEvent.setSent(false);
        recipientEvent.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(recipientEvent);

        String message = "Успешно переведено %s руб клиенту %s".formatted(amount.toPlainString(), recipientName);

        return new TransferResponse(message, newSenderBalance);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
