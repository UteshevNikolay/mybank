package com.my.pet.project.mybank.cash.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final AccountClient accountClient;
    private final CashOperationRepository cashOperationRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Transactional
    public CashResponse processCash(CashRequest request) {
        AccountResponse account = accountClient.getAccountById(request.accountId());
        CashOperationType type = "GET".equals(request.action())
                ? CashOperationType.WITHDRAWAL
                : CashOperationType.DEPOSIT;

        BigDecimal amount = request.value();
        BigDecimal currentBalance = account.balance();

        if (type == CashOperationType.WITHDRAWAL && currentBalance.compareTo(amount) < 0) {
            log.warn("Withdrawal failed - insufficient funds: accountId={}, requested={}, available={}", request.accountId(), amount, currentBalance);
            Counter.builder("cash.withdrawal.failed")
                    .tag("login", account.login())
                    .description("Failed withdrawal attempts due to insufficient funds")
                    .register(meterRegistry)
                    .increment();
            throw new InsufficientFundsException("Недостаточно средств на счету");
        }

        BigDecimal newBalance = type == CashOperationType.WITHDRAWAL
                ? currentBalance.subtract(amount)
                : currentBalance.add(amount);

        accountClient.updateBalance(request.accountId(), new BalanceUpdateRequest(newBalance, account.version()));

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
        event.setPayload(toJson(Map.of(
                "accountId", request.accountId(),
                "eventType", event.getEventType(),
                "message", message)));
        event.setSent(false);
        event.setCreatedAt(LocalDateTime.now());
        outboxEventRepository.save(event);

        log.info("Cash operation completed: accountId={}, type={}, amount={}, newBalance={}", request.accountId(), type, amount, newBalance);
        String responseMessage = type == CashOperationType.WITHDRAWAL
                ? "Снято %s руб".formatted(amount.toPlainString())
                : "Положено %s руб".formatted(amount.toPlainString());

        return new CashResponse(responseMessage, newBalance);
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event", e);
        }
    }
}
