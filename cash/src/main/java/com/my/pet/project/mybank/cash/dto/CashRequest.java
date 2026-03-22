package com.my.pet.project.mybank.cash.dto;

public record CashRequest(
        Long accountId,
        int value,
        String action
) {}
