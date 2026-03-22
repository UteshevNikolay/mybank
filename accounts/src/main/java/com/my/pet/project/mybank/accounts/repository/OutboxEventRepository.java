package com.my.pet.project.mybank.accounts.repository;

import com.my.pet.project.mybank.accounts.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findBySentFalse();
}
