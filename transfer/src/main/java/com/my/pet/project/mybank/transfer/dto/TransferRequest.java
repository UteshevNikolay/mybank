package com.my.pet.project.mybank.transfer.dto;

import java.math.BigDecimal;

public record TransferRequest(
        Long fromAccountId,
        String toLogin,
        BigDecimal value
) {}
