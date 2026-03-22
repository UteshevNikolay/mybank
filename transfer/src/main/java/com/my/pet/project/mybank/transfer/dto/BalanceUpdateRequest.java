package com.my.pet.project.mybank.transfer.dto;

import java.math.BigDecimal;

public record BalanceUpdateRequest(
        BigDecimal newBalance
) {}
