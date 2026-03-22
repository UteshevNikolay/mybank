package com.my.pet.project.mybank.frontend.dto;

import java.math.BigDecimal;

public record TransferRequest(
        Long fromAccountId,
        String toLogin,
        BigDecimal value
) {}
