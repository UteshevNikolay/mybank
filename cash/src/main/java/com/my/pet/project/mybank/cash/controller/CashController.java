package com.my.pet.project.mybank.cash.controller;

import com.my.pet.project.mybank.cash.dto.CashRequest;
import com.my.pet.project.mybank.cash.dto.CashResponse;
import com.my.pet.project.mybank.cash.service.CashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping
    public ResponseEntity<CashResponse> processCash(@Valid @RequestBody CashRequest request) {
        log.info("POST /cash requested: accountId={}, action={}", request.accountId(), request.action());
        CashResponse response = cashService.processCash(request);
        return ResponseEntity.ok(response);
    }
}
