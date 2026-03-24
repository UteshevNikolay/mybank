package com.my.pet.project.mybank.transfer.repository;

import com.my.pet.project.mybank.transfer.model.TransferOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {
}
