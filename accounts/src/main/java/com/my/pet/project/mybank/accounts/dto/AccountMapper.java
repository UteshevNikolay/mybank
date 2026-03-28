package com.my.pet.project.mybank.accounts.dto;

import com.my.pet.project.mybank.accounts.model.Account;

public final class AccountMapper {

    private AccountMapper() {}

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getLogin(),
                account.getFirstName(),
                account.getLastName(),
                account.getDateOfBirth(),
                account.getBalance(),
                account.getVersion()
        );
    }

    public static Account toEntity(AccountCreateRequest request) {
        Account account = new Account();
        account.setLogin(request.login());
        account.setFirstName(request.firstName());
        account.setLastName(request.lastName());
        account.setDateOfBirth(request.dateOfBirth());
        return account;
    }
}
