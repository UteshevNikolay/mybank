package com.my.pet.project.mybank.accounts.dto;

import java.math.BigDecimal;

public record BalanceUpdateRequest(
        BigDecimal newBalance
) {}
