package com.my.pet.project.mybank.frontend.client;

import com.my.pet.project.mybank.frontend.dto.TransferRequest;
import com.my.pet.project.mybank.frontend.dto.TransferResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferClient {

    private final RestClient transferRestClient;

    @CircuitBreaker(name = "transfer", fallbackMethod = "processTransferFallback")
    public TransferResponse processTransfer(Long fromAccountId, String toLogin, BigDecimal value) {
        TransferRequest request = new TransferRequest(fromAccountId, toLogin, value);
        return transferRestClient.post()
                .uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TransferResponse.class);
    }

    private TransferResponse processTransferFallback(Long fromAccountId, String toLogin, BigDecimal value, Throwable t) {
        log.warn("Transfer service unavailable: processTransfer(fromAccountId={})", fromAccountId, t);
        return null;
    }
}
