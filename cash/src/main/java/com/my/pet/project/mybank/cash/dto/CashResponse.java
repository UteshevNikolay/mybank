package com.my.pet.project.mybank.cash.dto;

import java.math.BigDecimal;

public record CashResponse(
        String message,
        BigDecimal newBalance
) {}
