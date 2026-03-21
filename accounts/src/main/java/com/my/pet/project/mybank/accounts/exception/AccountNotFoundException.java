package com.my.pet.project.mybank.accounts.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(Long id) {
        super("Account not found with id: " + id);
    }

    public AccountNotFoundException(String login) {
        super("Account not found with login: " + login);
    }
}
