package com.my.pet.project.mybank.frontend.dto;

import java.math.BigDecimal;

public record CashRequest(
        Long accountId,
        BigDecimal value,
        String action
) {}
