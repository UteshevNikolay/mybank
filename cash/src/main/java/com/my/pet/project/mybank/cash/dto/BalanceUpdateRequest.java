package com.my.pet.project.mybank.cash.dto;

import java.math.BigDecimal;

public record BalanceUpdateRequest(
        BigDecimal newBalance,
        Long version
) {}
