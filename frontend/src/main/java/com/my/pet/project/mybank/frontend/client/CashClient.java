package com.my.pet.project.mybank.frontend.client;

import com.my.pet.project.mybank.frontend.dto.CashRequest;
import com.my.pet.project.mybank.frontend.dto.CashResponse;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CashClient {

    private final RestClient cashRestClient;

    @CircuitBreaker(name = "cash", fallbackMethod = "processCashFallback")
    public CashResponse processCash(Long accountId, BigDecimal value, String action) {
        CashRequest request = new CashRequest(accountId, value, action);
        return cashRestClient.post()
                .uri("/cash")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(CashResponse.class);
    }

    private CashResponse processCashFallback(Long accountId, BigDecimal value, String action, Throwable t) {
        throw new ServiceException("Cash service unavailable", t);
    }
}
