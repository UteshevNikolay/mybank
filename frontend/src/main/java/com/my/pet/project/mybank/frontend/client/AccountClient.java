package com.my.pet.project.mybank.frontend.client;

import com.my.pet.project.mybank.frontend.dto.AccountResponse;
import com.my.pet.project.mybank.frontend.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.frontend.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountClient {

    private final RestClient restClient;

    @CircuitBreaker(name = "accounts", fallbackMethod = "getAccountByLoginFallback")
    public AccountResponse getAccountByLogin(String login) {
        return restClient.get()
                .uri("/accounts/accounts/login/{login}", login)
                .retrieve()
                .body(AccountResponse.class);
    }

    private AccountResponse getAccountByLoginFallback(String login, Throwable t) {
        throw new ServiceException("Accounts service unavailable", t);
    }

    @CircuitBreaker(name = "accounts", fallbackMethod = "updateAccountFallback")
    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {
        return restClient.put()
                .uri("/accounts/accounts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }

    private AccountResponse updateAccountFallback(Long id, AccountUpdateRequest request, Throwable t) {
        throw new ServiceException("Accounts service unavailable", t);
    }

    @CircuitBreaker(name = "accounts", fallbackMethod = "getAllAccountsFallback")
    public List<AccountResponse> getAllAccounts() {
        return restClient.get()
                .uri("/accounts/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    private List<AccountResponse> getAllAccountsFallback(Throwable t) {
        throw new ServiceException("Accounts service unavailable", t);
    }

    @CircuitBreaker(name = "accounts", fallbackMethod = "updateBalanceFallback")
    public AccountResponse updateBalance(Long id, BalanceUpdateRequest request) {
        return restClient.patch()
                .uri("/accounts/accounts/{id}/balance", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }

    private AccountResponse updateBalanceFallback(Long id, BalanceUpdateRequest request, Throwable t) {
        throw new ServiceException("Accounts service unavailable", t);
    }
}
