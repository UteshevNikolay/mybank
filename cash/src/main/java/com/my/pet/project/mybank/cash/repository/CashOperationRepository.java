package com.my.pet.project.mybank.cash.repository;

import com.my.pet.project.mybank.cash.model.CashOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {
}
