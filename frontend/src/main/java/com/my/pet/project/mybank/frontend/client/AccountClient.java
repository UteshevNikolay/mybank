package com.my.pet.project.mybank.frontend.client;

import com.my.pet.project.mybank.frontend.dto.AccountResponse;
import com.my.pet.project.mybank.frontend.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.frontend.dto.BalanceUpdateRequest;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
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

    public AccountResponse getAccountByLogin(String login) {
        try {
            return restClient.get()
                    .uri("/accounts/accounts/login/{login}", login)
                    .retrieve()
                    .body(AccountResponse.class);
        } catch (Exception e) {
            throw new ServiceException("Failed to get account by login: " + login, e);
        }
    }

    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {
        try {
            return restClient.put()
                    .uri("/accounts/accounts/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AccountResponse.class);
        } catch (Exception e) {
            throw new ServiceException("Failed to update account: " + id, e);
        }
    }

    public List<AccountResponse> getAllAccounts() {
        try {
            return restClient.get()
                    .uri("/accounts/accounts")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new ServiceException("Failed to get all accounts", e);
        }
    }

    public AccountResponse updateBalance(Long id, BalanceUpdateRequest request) {
        try {
            return restClient.patch()
                    .uri("/accounts/accounts/{id}/balance", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AccountResponse.class);
        } catch (Exception e) {
            throw new ServiceException("Failed to update balance for account: " + id, e);
        }
    }
}
