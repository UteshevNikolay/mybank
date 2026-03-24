package com.my.pet.project.mybank.transfer.dto;

import java.math.BigDecimal;

public record TransferResponse(
        String message,
        BigDecimal newBalance
) {}
