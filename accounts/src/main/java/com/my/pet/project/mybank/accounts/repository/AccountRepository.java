package com.my.pet.project.mybank.accounts.repository;

import com.my.pet.project.mybank.accounts.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByLogin(String login);
}
