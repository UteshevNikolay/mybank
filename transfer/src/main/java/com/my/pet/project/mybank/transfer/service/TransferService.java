package com.my.pet.project.mybank.transfer.service;

import com.my.pet.project.mybank.transfer.client.AccountClient;
import com.my.pet.project.mybank.transfer.dto.AccountResponse;
import com.my.pet.project.mybank.transfer.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.transfer.dto.TransferRequest;
import com.my.pet.project.mybank.transfer.dto.TransferResponse;
import com.my.pet.project.mybank.transfer.exception.InsufficientFundsException;
import com.my.pet.project.mybank.transfer.model.TransferOperation;
import com.my.pet.project.mybank.transfer.repository.TransferOperationRepository;
import com.my.pet.project.mybank.transfer.stub.NotificationStub;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountClient accountClient;
    private final TransferOperationRepository transferOperationRepository;
    private final NotificationStub notificationStub;

    @Transactional
    public TransferResponse processTransfer(TransferRequest request) {
        AccountResponse sender = accountClient.getAccountById(request.fromAccountId());
        BigDecimal amount = BigDecimal.valueOf(request.value());

        if (sender.balance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Недостаточно средств на счету");
        }

        AccountResponse recipient = accountClient.getAccountByLogin(request.toLogin());

        BigDecimal newSenderBalance = sender.balance().subtract(amount);
        accountClient.updateBalance(sender.id(), new BalanceUpdateRequest(newSenderBalance));

        BigDecimal newRecipientBalance = recipient.balance().add(amount);
        accountClient.updateBalance(recipient.id(), new BalanceUpdateRequest(newRecipientBalance));

        TransferOperation operation = new TransferOperation();
        operation.setFromAccountId(sender.id());
        operation.setToAccountId(recipient.id());
        operation.setAmount(amount);
        operation.setCreatedAt(LocalDateTime.now());
        transferOperationRepository.save(operation);

        notificationStub.notifyTransfer(sender.id(), recipient.id(), amount);

        String recipientName = recipient.lastName() + " " + recipient.firstName();
        String message = "Успешно переведено %d руб клиенту %s".formatted(request.value(), recipientName);

        return new TransferResponse(message, newSenderBalance);
    }
}
