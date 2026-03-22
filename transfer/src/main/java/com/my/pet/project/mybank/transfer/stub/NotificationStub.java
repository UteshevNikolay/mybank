package com.my.pet.project.mybank.transfer.stub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class NotificationStub {

    // TODO: replace with real HTTP call to Notifications service
    public void notifyTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        log.info("Notification: Transfer - fromAccountId={}, toAccountId={}, amount={}", fromAccountId, toAccountId, amount);
    }
}
