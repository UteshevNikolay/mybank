package com.my.pet.project.mybank.transfer.client;

import com.my.pet.project.mybank.transfer.dto.AccountResponse;
import com.my.pet.project.mybank.transfer.dto.BalanceUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AccountClient {

    private final RestClient accountsRestClient;

    public AccountResponse getAccountById(Long id) {
        return accountsRestClient.get()
                .uri("/accounts/{id}", id)
                .retrieve()
                .body(AccountResponse.class);
    }

    public AccountResponse getAccountByLogin(String login) {
        return accountsRestClient.get()
                .uri("/accounts/login/{login}", login)
                .retrieve()
                .body(AccountResponse.class);
    }

    public AccountResponse updateBalance(Long id, BalanceUpdateRequest request) {
        return accountsRestClient.patch()
                .uri("/accounts/{id}/balance", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }
}
