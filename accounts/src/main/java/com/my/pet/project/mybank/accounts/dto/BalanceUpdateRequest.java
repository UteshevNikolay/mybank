package com.my.pet.project.mybank.accounts.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record BalanceUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal newBalance,
        @NotNull Long version
) {}
