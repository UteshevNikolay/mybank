package com.my.pet.project.mybank.frontend.controller;

import com.my.pet.project.mybank.frontend.client.AccountClient;
import com.my.pet.project.mybank.frontend.client.CashClient;
import com.my.pet.project.mybank.frontend.client.TransferClient;
import com.my.pet.project.mybank.frontend.dto.AccountDto;
import com.my.pet.project.mybank.frontend.dto.AccountResponse;
import com.my.pet.project.mybank.frontend.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.frontend.dto.CashAction;
import com.my.pet.project.mybank.frontend.dto.CashResponse;
import com.my.pet.project.mybank.frontend.dto.TransferResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Controller
public class MainController {

    private final AccountClient accountClient;
    private final CashClient cashClient;
    private final TransferClient transferClient;

    public MainController(AccountClient accountClient,
                          CashClient cashClient,
                          TransferClient transferClient) {
        this.accountClient = accountClient;
        this.cashClient = cashClient;
        this.transferClient = transferClient;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String login = oidcUser.getPreferredUsername();
        AccountResponse account = accountClient.getAccountByLogin(login);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }
        List<AccountResponse> allAccounts = accountClient.getAllAccounts();
        fillModel(model, account, allAccounts, login, null, null);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(Model model,
                              @RequestParam("name") String name,
                              @RequestParam("birthdate") LocalDate birthdate,
                              @AuthenticationPrincipal OidcUser oidcUser) {
        String login = oidcUser.getPreferredUsername();
        AccountResponse account = accountClient.getAccountByLogin(login);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }
        String[] parts = name.split(" ", 2);
        String lastName = parts[0];
        String firstName = parts.length > 1 ? parts[1] : "";

        AccountUpdateRequest request = new AccountUpdateRequest(firstName, lastName, birthdate);
        account = accountClient.updateAccount(account.id(), request);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }

        List<AccountResponse> allAccounts = accountClient.getAllAccounts();
        fillModel(model, account, allAccounts, login, null, null);
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(Model model,
                           @RequestParam("value") BigDecimal value,
                           @RequestParam("action") CashAction action,
                           @AuthenticationPrincipal OidcUser oidcUser) {
        String login = oidcUser.getPreferredUsername();
        AccountResponse account = accountClient.getAccountByLogin(login);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }
        CashResponse cashResponse = cashClient.processCash(account.id(), value, action.name());
        if (cashResponse == null) {
            List<AccountResponse> allAccounts = accountClient.getAllAccounts();
            fillModel(model, account, allAccounts, login, List.of("Сервис временно недоступен"), null);
            return "main";
        }
        account = accountClient.getAccountByLogin(login);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }
        List<AccountResponse> allAccounts = accountClient.getAllAccounts();
        fillModel(model, account, allAccounts, login, null, cashResponse.message());
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(Model model,
                           @RequestParam("value") BigDecimal value,
                           @RequestParam("login") String toLogin,
                           @AuthenticationPrincipal OidcUser oidcUser) {
        String login = oidcUser.getPreferredUsername();
        AccountResponse account = accountClient.getAccountByLogin(login);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }
        TransferResponse transferResponse = transferClient.processTransfer(account.id(), toLogin, value);
        if (transferResponse == null) {
            List<AccountResponse> allAccounts = accountClient.getAllAccounts();
            fillModel(model, account, allAccounts, login, List.of("Сервис временно недоступен"), null);
            return "main";
        }
        account = accountClient.getAccountByLogin(login);
        if (account == null) {
            fillEmptyModel(model, List.of("Сервис временно недоступен"));
            return "main";
        }
        List<AccountResponse> allAccounts = accountClient.getAllAccounts();
        fillModel(model, account, allAccounts, login, null, transferResponse.message());
        return "main";
    }

    private void fillModel(Model model, AccountResponse account, List<AccountResponse> allAccounts,
                           String currentLogin, List<String> errors, String info) {
        model.addAttribute("name", account.lastName() + " " + account.firstName());
        model.addAttribute("birthdate", account.dateOfBirth() != null
                ? account.dateOfBirth().format(DateTimeFormatter.ISO_DATE) : "");
        model.addAttribute("sum", account.balance().intValue());

        List<AccountDto> accountDtos = allAccounts.stream()
                .filter(a -> !a.login().equals(currentLogin))
                .map(a -> new AccountDto(a.login(), a.lastName() + " " + a.firstName()))
                .toList();
        model.addAttribute("accounts", accountDtos);
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }

    private void fillEmptyModel(Model model, List<String> errors) {
        model.addAttribute("name", "");
        model.addAttribute("birthdate", "");
        model.addAttribute("sum", 0);
        model.addAttribute("accounts", Collections.emptyList());
        model.addAttribute("errors", errors);
        model.addAttribute("info", null);
    }
}
