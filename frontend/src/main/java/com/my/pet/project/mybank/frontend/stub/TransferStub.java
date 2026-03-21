package com.my.pet.project.mybank.frontend.stub;

import com.my.pet.project.mybank.frontend.client.AccountClient;
import com.my.pet.project.mybank.frontend.dto.AccountResponse;
import com.my.pet.project.mybank.frontend.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransferStub {

    private final AccountClient accountClient;

    public String processTransfer(Long fromAccountId, BigDecimal fromBalance, String toLogin, int value) {
        if (fromBalance.intValue() < value) {
            throw new ServiceException("Недостаточно средств на счету");
        }

        AccountResponse recipient = accountClient.getAccountByLogin(toLogin);

        BigDecimal newFromBalance = fromBalance.subtract(BigDecimal.valueOf(value));
        accountClient.updateBalance(fromAccountId, new BalanceUpdateRequest(newFromBalance));

        BigDecimal newToBalance = recipient.balance().add(BigDecimal.valueOf(value));
        accountClient.updateBalance(recipient.id(), new BalanceUpdateRequest(newToBalance));

        String recipientName = recipient.lastName() + " " + recipient.firstName();
        return "Успешно переведено %d руб клиенту %s".formatted(value, recipientName);
    }
}
