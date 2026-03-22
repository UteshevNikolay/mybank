package com.my.pet.project.mybank.frontend.client;

import com.my.pet.project.mybank.frontend.dto.TransferRequest;
import com.my.pet.project.mybank.frontend.dto.TransferResponse;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class TransferClient {

    private final RestClient restClient;

    public TransferResponse processTransfer(Long fromAccountId, String toLogin, int value) {
        try {
            TransferRequest request = new TransferRequest(fromAccountId, toLogin, value);
            return restClient.post()
                    .uri("/transfer/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TransferResponse.class);
        } catch (Exception e) {
            throw new ServiceException("Failed to process transfer", e);
        }
    }
}
