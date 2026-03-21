package com.my.pet.project.mybank.frontend.dto;

public record CashRequest(
        Long accountId,
        int value,
        String action
) {}
