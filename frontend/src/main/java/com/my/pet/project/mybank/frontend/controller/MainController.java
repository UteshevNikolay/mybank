package com.my.pet.project.mybank.frontend.controller;

import com.my.pet.project.mybank.frontend.client.AccountClient;
import com.my.pet.project.mybank.frontend.dto.AccountDto;
import com.my.pet.project.mybank.frontend.dto.AccountResponse;
import com.my.pet.project.mybank.frontend.dto.AccountUpdateRequest;
import com.my.pet.project.mybank.frontend.dto.CashAction;
import com.my.pet.project.mybank.frontend.exception.ServiceException;
import com.my.pet.project.mybank.frontend.stub.CashStub;
import com.my.pet.project.mybank.frontend.stub.TransferStub;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Controller
public class MainController {

    private final AccountClient accountClient;
    private final CashStub cashStub;
    private final TransferStub transferStub;
    private final String defaultLogin;

    public MainController(AccountClient accountClient,
                          CashStub cashStub,
                          TransferStub transferStub,
                          @Value("${frontend.default-login}") String defaultLogin) {
        this.accountClient = accountClient;
        this.cashStub = cashStub;
        this.transferStub = transferStub;
        this.defaultLogin = defaultLogin;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model) {
        try {
            AccountResponse account = accountClient.getAccountByLogin(defaultLogin);
            List<AccountResponse> allAccounts = accountClient.getAllAccounts();
            fillModel(model, account, allAccounts, null, null);
        } catch (ServiceException e) {
            fillEmptyModel(model, List.of(e.getMessage()));
        }
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(Model model,
                              @RequestParam("name") String name,
                              @RequestParam("birthdate") LocalDate birthdate) {
        try {
            AccountResponse account = accountClient.getAccountByLogin(defaultLogin);
            String[] parts = name.split(" ", 2);
            String lastName = parts[0];
            String firstName = parts.length > 1 ? parts[1] : "";

            AccountUpdateRequest request = new AccountUpdateRequest(firstName, lastName, birthdate);
            account = accountClient.updateAccount(account.id(), request);

            List<AccountResponse> allAccounts = accountClient.getAllAccounts();
            fillModel(model, account, allAccounts, null, null);
        } catch (ServiceException e) {
            fillEmptyModel(model, List.of(e.getMessage()));
        }
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(Model model,
                           @RequestParam("value") int value,
                           @RequestParam("action") CashAction action) {
        try {
            AccountResponse account = accountClient.getAccountByLogin(defaultLogin);
            String info = cashStub.processCash(account.id(), account.balance(), value, action);
            account = accountClient.getAccountByLogin(defaultLogin);
            List<AccountResponse> allAccounts = accountClient.getAllAccounts();
            fillModel(model, account, allAccounts, null, info);
        } catch (ServiceException e) {
            try {
                AccountResponse account = accountClient.getAccountByLogin(defaultLogin);
                List<AccountResponse> allAccounts = accountClient.getAllAccounts();
                fillModel(model, account, allAccounts, List.of(e.getMessage()), null);
            } catch (ServiceException ex) {
                fillEmptyModel(model, List.of(e.getMessage()));
            }
        }
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(Model model,
                           @RequestParam("value") int value,
                           @RequestParam("login") String login) {
        try {
            AccountResponse account = accountClient.getAccountByLogin(defaultLogin);
            String info = transferStub.processTransfer(account.id(), account.balance(), login, value);
            account = accountClient.getAccountByLogin(defaultLogin);
            List<AccountResponse> allAccounts = accountClient.getAllAccounts();
            fillModel(model, account, allAccounts, null, info);
        } catch (ServiceException e) {
            try {
                AccountResponse account = accountClient.getAccountByLogin(defaultLogin);
                List<AccountResponse> allAccounts = accountClient.getAllAccounts();
                fillModel(model, account, allAccounts, List.of(e.getMessage()), null);
            } catch (ServiceException ex) {
                fillEmptyModel(model, List.of(e.getMessage()));
            }
        }
        return "main";
    }

    private void fillModel(Model model, AccountResponse account, List<AccountResponse> allAccounts,
                           List<String> errors, String info) {
        model.addAttribute("name", account.lastName() + " " + account.firstName());
        model.addAttribute("birthdate", account.dateOfBirth() != null
                ? account.dateOfBirth().format(DateTimeFormatter.ISO_DATE) : "");
        model.addAttribute("sum", account.balance().intValue());

        List<AccountDto> accountDtos = allAccounts.stream()
                .filter(a -> !a.login().equals(defaultLogin))
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
