package com.my.pet.project.mybank.cash.dto;

import java.math.BigDecimal;

public record CashRequest(
        Long accountId,
        BigDecimal value,
        String action
) {}
