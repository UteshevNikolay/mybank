package com.my.pet.project.mybank.frontend.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String message,
        BigDecimal newBalance
) {}
