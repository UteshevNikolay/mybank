package com.my.pet.project.mybank.frontend.dto;

import java.math.BigDecimal;

public record CashResponse(
        String message,
        BigDecimal newBalance
) {}
