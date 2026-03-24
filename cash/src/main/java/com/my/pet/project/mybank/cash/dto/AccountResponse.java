package com.my.pet.project.mybank.cash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountResponse(
        Long id,
        String login,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        BigDecimal balance
) {}
