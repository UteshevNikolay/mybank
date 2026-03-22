package com.my.pet.project.mybank.frontend.dto;

public record TransferRequest(
        Long fromAccountId,
        String toLogin,
        int value
) {}
