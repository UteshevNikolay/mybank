package com.my.pet.project.mybank.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AccountUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull LocalDate dateOfBirth
) {}
