package com.my.pet.project.mybank.frontend.stub;

import com.my.pet.project.mybank.frontend.client.AccountClient;
import com.my.pet.project.mybank.frontend.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.frontend.dto.CashAction;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashStub {

    private final AccountClient accountClient;

    public String processCash(Long accountId, BigDecimal currentBalance, int value, CashAction action) {
        if (action == CashAction.GET && currentBalance.intValue() < value) {
            throw new ServiceException("Недостаточно средств на счету");
        }

        BigDecimal newBalance = action == CashAction.GET
                ? currentBalance.subtract(BigDecimal.valueOf(value))
                : currentBalance.add(BigDecimal.valueOf(value));

        accountClient.updateBalance(accountId, new BalanceUpdateRequest(newBalance));

        return action == CashAction.GET
                ? "Снято %d руб".formatted(value)
                : "Положено %d руб".formatted(value);
    }
}
