package com.fatmanur.ecommerce.transaction.repository;

import com.fatmanur.ecommerce.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<Transaction> findAllByOrderByCreatedAtDesc();
}
