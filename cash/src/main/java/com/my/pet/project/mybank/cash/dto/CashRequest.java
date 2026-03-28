package com.my.pet.project.mybank.cash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CashRequest(
        @NotNull Long accountId,
        @NotNull @Positive BigDecimal value,
        @NotBlank @Pattern(regexp = "PUT|GET") String action
) {}
