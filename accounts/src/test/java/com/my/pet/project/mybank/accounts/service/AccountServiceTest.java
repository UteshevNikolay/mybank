package com.my.pet.project.mybank.accounts.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.pet.project.mybank.accounts.dto.AccountCreateRequest;
import com.my.pet.project.mybank.accounts.dto.AccountResponse;
import com.my.pet.project.mybank.accounts.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.accounts.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.accounts.exception.AccountNotFoundException;
import com.my.pet.project.mybank.accounts.model.Account;
import com.my.pet.project.mybank.accounts.model.OutboxEvent;
import com.my.pet.project.mybank.accounts.repository.AccountRepository;
import com.my.pet.project.mybank.accounts.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AccountService accountService;

    @Test
    void createAccount_success() {
        AccountCreateRequest request = new AccountCreateRequest(
                "ivan.petrov", "Ivan", "Petrov", LocalDate.of(1990, 1, 15)
        );

        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account a = invocation.getArgument(0);
            a.setId(1L);
            return a;
        });

        AccountResponse response = accountService.createAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.login()).isEqualTo("ivan.petrov");
        assertThat(response.firstName()).isEqualTo("Ivan");
        assertThat(response.lastName()).isEqualTo("Petrov");
        assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);

        verify(accountRepository).save(any(Account.class));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent capturedEvent = outboxCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("ACCOUNT_CREATED");
        assertThat(capturedEvent.getPayload()).contains("ACCOUNT_CREATED");
        assertThat(capturedEvent.isSent()).isFalse();
        assertThat(capturedEvent.getCreatedAt()).isNotNull();
    }

    @Test
    void getAccountById_found() {
        Account account = new Account(2L, "maria.sidorova", "Maria", "Sidorova",
                LocalDate.of(1985, 6, 20), new BigDecimal("500.00"));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountById(2L);

        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.login()).isEqualTo("maria.sidorova");
        assertThat(response.firstName()).isEqualTo("Maria");
        assertThat(response.lastName()).isEqualTo("Sidorova");
        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void getAccountById_notFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAccountByLogin_found() {
        Account account = new Account(3L, "alex.smirnov", "Alex", "Smirnov",
                LocalDate.of(1992, 3, 10), new BigDecimal("1000.00"));
        when(accountRepository.findByLogin("alex.smirnov")).thenReturn(Optional.of(account));

        AccountResponse response = accountService.getAccountByLogin("alex.smirnov");

        assertThat(response.id()).isEqualTo(3L);
        assertThat(response.login()).isEqualTo("alex.smirnov");
        assertThat(response.firstName()).isEqualTo("Alex");
    }

    @Test
    void getAccountByLogin_notFound() {
        when(accountRepository.findByLogin("unknown.user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountByLogin("unknown.user"))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("unknown.user");
    }

    @Test
    void getAllAccounts_returnsAll() {
        Account first = new Account(1L, "user.one", "User", "One",
                LocalDate.of(1991, 4, 5), BigDecimal.ZERO);
        Account second = new Account(2L, "user.two", "User", "Two",
                LocalDate.of(1993, 7, 12), new BigDecimal("250.00"));
        when(accountRepository.findAll()).thenReturn(List.of(first, second));

        List<AccountResponse> responses = accountService.getAllAccounts();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).login()).isEqualTo("user.one");
        assertThat(responses.get(1).login()).isEqualTo("user.two");
    }

    @Test
    void updateAccount_success() {
        Account existing = new Account(5L, "old.login", "OldFirst", "OldLast",
                LocalDate.of(1988, 2, 28), new BigDecimal("100.00"));
        AccountUpdateRequest request = new AccountUpdateRequest(
                "NewFirst", "NewLast", LocalDate.of(1989, 5, 17)
        );

        when(accountRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.updateAccount(5L, request);

        assertThat(response.firstName()).isEqualTo("NewFirst");
        assertThat(response.lastName()).isEqualTo("NewLast");
        assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1989, 5, 17));

        verify(accountRepository).save(existing);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent capturedEvent = outboxCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("ACCOUNT_UPDATED");
        assertThat(capturedEvent.getPayload()).contains("ACCOUNT_UPDATED");
        assertThat(capturedEvent.isSent()).isFalse();
        assertThat(capturedEvent.getCreatedAt()).isNotNull();
    }

    @Test
    void updateBalance_success() {
        Account existing = new Account(7L, "balance.user", "Balance", "User",
                LocalDate.of(1995, 9, 3), new BigDecimal("0.00"));
        BalanceUpdateRequest request = new BalanceUpdateRequest(new BigDecimal("750.50"));

        when(accountRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountResponse response = accountService.updateBalance(7L, request);

        assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("750.50"));

        verify(accountRepository).save(existing);

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent capturedEvent = outboxCaptor.getValue();
        assertThat(capturedEvent.getEventType()).isEqualTo("BALANCE_UPDATED");
        assertThat(capturedEvent.getPayload()).contains("BALANCE_UPDATED");
        assertThat(capturedEvent.getPayload()).contains("750.50");
        assertThat(capturedEvent.isSent()).isFalse();
        assertThat(capturedEvent.getCreatedAt()).isNotNull();
    }
}
