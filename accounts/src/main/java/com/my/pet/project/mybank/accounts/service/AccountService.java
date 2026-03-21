package com.my.pet.project.mybank.accounts.service;

import com.my.pet.project.mybank.accounts.dto.AccountCreateRequest;
import com.my.pet.project.mybank.accounts.dto.AccountMapper;
import com.my.pet.project.mybank.accounts.dto.AccountResponse;
import com.my.pet.project.mybank.accounts.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.accounts.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.accounts.exception.AccountNotFoundException;
import com.my.pet.project.mybank.accounts.model.Account;
import com.my.pet.project.mybank.accounts.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        Account account = AccountMapper.toEntity(request);
        account.setBalance(BigDecimal.ZERO);
        Account saved = accountRepository.save(account);
        return AccountMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByLogin(String login) {
        Account account = accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
        return AccountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountMapper::toResponse)
                .toList();
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setDateOfBirth(request.dateOfBirth());
        Account saved = accountRepository.save(account);
        return AccountMapper.toResponse(saved);
    }

    @Transactional
    public AccountResponse updateBalance(Long id, BalanceUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        account.setBalance(request.newBalance());
        Account saved = accountRepository.save(account);
        return AccountMapper.toResponse(saved);
    }
}
