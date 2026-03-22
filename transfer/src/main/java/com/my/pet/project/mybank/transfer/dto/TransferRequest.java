package com.my.pet.project.mybank.transfer.dto;

public record TransferRequest(
        Long fromAccountId,
        String toLogin,
        int value
) {}
