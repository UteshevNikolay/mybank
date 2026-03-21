package com.my.pet.project.mybank.cash.stub;

import com.my.pet.project.mybank.cash.model.CashOperationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class NotificationStub {

    // TODO: replace with real HTTP call to Notifications service
    public void notifyCashOperation(Long accountId, BigDecimal amount, CashOperationType type) {
        log.info("Notification: Cash operation - accountId={}, amount={}, type={}", accountId, amount, type);
    }
}
