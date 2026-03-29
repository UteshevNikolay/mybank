package com.my.pet.project.mybank.accounts.controller;

import com.my.pet.project.mybank.accounts.dto.AccountCreateRequest;
import com.my.pet.project.mybank.accounts.dto.AccountResponse;
import com.my.pet.project.mybank.accounts.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.accounts.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.accounts.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        log.info("POST /accounts requested: login={}", request.login());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        log.info("GET /accounts/{} requested", id);
        return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @GetMapping("/login/{login}")
    public ResponseEntity<AccountResponse> getAccountByLogin(@PathVariable String login) {
        log.info("GET /accounts/login/{} requested", login);
        return ResponseEntity.ok(accountService.getAccountByLogin(login));
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        log.info("GET /accounts requested");
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateAccount(@PathVariable Long id,
                                                         @Valid @RequestBody AccountUpdateRequest request) {
        log.info("PUT /accounts/{} requested", id);
        return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

    @PatchMapping("/{id}/balance")
    public ResponseEntity<AccountResponse> updateBalance(@PathVariable Long id,
                                                         @Valid @RequestBody BalanceUpdateRequest request) {
        log.info("PATCH /accounts/{}/balance requested", id);
        return ResponseEntity.ok(accountService.updateBalance(id, request));
    }
}
