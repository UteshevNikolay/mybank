package com.my.pet.project.mybank.cash.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountClient accountClient;
    private final CashOperationRepository cashOperationRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public CashResponse processCash(CashRequest request) {
        AccountResponse account = accountClient.getAccountById(request.accountId());
        CashOperationType type = "GET".equals(request.action())
                ? CashOperationType.WITHDRAWAL
                : CashOperationType.DEPOSIT;

        BigDecimal amount = BigDecimal.valueOf(request.value());
        BigDecimal currentBalance = account.balance();

        if (type == CashOperationType.WITHDRAWAL && currentBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на счету");
        }

        BigDecimal newBalance = type == CashOperationType.WITHDRAWAL
                ? currentBalance.subtract(amount)
                : currentBalance.add(amount);

        accountClient.updateBalance(request.accountId(), new BalanceUpdateRequest(newBalance));

        CashOperation operation = new CashOperation();
        operation.setAccountId(request.accountId());
        operation.setAmount(amount);
        operation.setType(type);
        operation.setCreatedAt(LocalDateTime.now());
        cashOperationRepository.save(operation);

        OutboxEvent event = new OutboxEvent();
        event.setEventType(type == CashOperationType.DEPOSIT ? "CASH_DEPOSIT" : "CASH_WITHDRAWAL");
        String message = type == CashOperationType.DEPOSIT
                ? "Пополнение счёта на %s руб".formatted(amount.toPlainString())
                : "Снятие со счёта %s руб".formatted(amount.toPlainString());
        event.setPayload("{\"accountId\":%d,\"eventType\":\"%s\",\"message\":\"%s\"}".formatted(
                request.accountId(), event.getEventType(), message));
        event.setSent(false);
        event.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(event);

        String responseMessage = type == CashOperationType.WITHDRAWAL
                ? "Снято %d руб".formatted(request.value())
                : "Положено %d руб".formatted(request.value());

        return new CashResponse(responseMessage, newBalance);
    }
}
