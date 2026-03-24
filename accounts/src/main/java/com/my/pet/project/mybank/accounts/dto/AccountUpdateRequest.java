package com.my.pet.project.mybank.accounts.dto;

import java.time.LocalDate;

public record AccountUpdateRequest(
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
