package com.my.pet.project.mybank.frontend.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.pet.project.mybank.frontend.dto.TransferRequest;
import com.my.pet.project.mybank.frontend.dto.TransferResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

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
                .exchange((req, res) -> {
                    if (res.getStatusCode().is4xxClientError()) {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> errorBody = mapper.readValue(res.getBody(), new TypeReference<>() {});
                        String message = (String) errorBody.getOrDefault("message", "Ошибка при переводе");
                        return new TransferResponse(message, null);
                    }
                    return res.bodyTo(TransferResponse.class);
                });
    }

    private TransferResponse processTransferFallback(Long fromAccountId, String toLogin, BigDecimal value, Throwable t) {
        log.warn("Transfer service unavailable: processTransfer(fromAccountId={})", fromAccountId, t);
        return null;
    }
}
