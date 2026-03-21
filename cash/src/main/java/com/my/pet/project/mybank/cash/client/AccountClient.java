package com.my.pet.project.mybank.cash.client;

import com.my.pet.project.mybank.cash.dto.AccountResponse;
import com.my.pet.project.mybank.cash.dto.BalanceUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class AccountClient {

    private final RestClient restClient;

    public AccountResponse getAccountById(Long id) {
        return restClient.get()
                .uri("/accounts/{id}", id)
                .retrieve()
                .body(AccountResponse.class);
    }

    public AccountResponse updateBalance(Long id, BalanceUpdateRequest request) {
        return restClient.patch()
                .uri("/accounts/{id}/balance", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AccountResponse.class);
    }
}
