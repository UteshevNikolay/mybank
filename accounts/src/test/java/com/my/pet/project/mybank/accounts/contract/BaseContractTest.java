package com.my.pet.project.mybank.accounts.contract;

import com.my.pet.project.mybank.accounts.controller.AccountController;
import com.my.pet.project.mybank.accounts.dto.AccountResponse;
import com.my.pet.project.mybank.accounts.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.accounts.service.AccountService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseContractTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(accountController);

        AccountResponse account = new AccountResponse(
                1L, "user1", "John", "Doe",
                LocalDate.of(1990, 1, 1), new BigDecimal("500.00"));

        AccountResponse updatedAccount = new AccountResponse(
                1L, "user1", "John", "Doe",
                LocalDate.of(1990, 1, 1), new BigDecimal("600.00"));

        when(accountService.getAccountById(1L)).thenReturn(account);
        when(accountService.getAccountByLogin("user1")).thenReturn(account);
        when(accountService.updateBalance(eq(1L), any(BalanceUpdateRequest.class)))
                .thenReturn(updatedAccount);
    }
}
