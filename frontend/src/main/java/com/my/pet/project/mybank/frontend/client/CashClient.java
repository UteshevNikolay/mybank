package com.my.pet.project.mybank.frontend.client;

import com.my.pet.project.mybank.frontend.dto.CashRequest;
import com.my.pet.project.mybank.frontend.dto.CashResponse;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CashClient {

    private final RestClient restClient;

    public CashResponse processCash(Long accountId, int value, String action) {
        try {
            CashRequest request = new CashRequest(accountId, value, action);
            return restClient.post()
                    .uri("/cash/cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CashResponse.class);
        } catch (Exception e) {
            throw new ServiceException("Failed to process cash operation", e);
        }
    }
}
