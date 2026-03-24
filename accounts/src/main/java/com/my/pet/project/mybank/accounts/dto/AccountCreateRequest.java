package com.my.pet.project.mybank.accounts.dto;

import java.time.LocalDate;

public record AccountCreateRequest(
        String login,
        String firstName,
        String lastName,
        LocalDate dateOfBirth
) {}
