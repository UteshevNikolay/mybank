package com.my.pet.project.mybank.cash.client;

import com.my.pet.project.mybank.cash.dto.AccountResponse;
import com.my.pet.project.mybank.cash.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.cash.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountClient {

    private final RestClient accountsRestClient;

    @CircuitBreaker(name = "accounts", fallbackMethod = "getAccountByIdFallback")
    public AccountResponse getAccountById(Long id) {
        return accountsRestClient.get()
                .uri("/accounts/{id}", id)
                .retrieve()
                .body(AccountResponse.class);
    }

    private AccountResponse getAccountByIdFallback(Long id, Throwable t) {
        log.error("Failed to get account by id={}: {}", id, t.getMessage(), t);
        throw new ServiceUnavailableException("Accounts service unavailable", t);
    }

    @CircuitBreaker(name = "accounts", fallbackMethod = "updateBalanceFallback")
    public AccountResponse updateBalance(Long id, BalanceUpdateRequest request) {
        return accountsRestClient.patch()
                .uri("/accounts/{id}/balance", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }

    private AccountResponse updateBalanceFallback(Long id, BalanceUpdateRequest request, Throwable t) {
        log.error("Failed to update balance for id={}: {}", id, t.getMessage(), t);
        throw new ServiceUnavailableException("Accounts service unavailable", t);
    }
}
