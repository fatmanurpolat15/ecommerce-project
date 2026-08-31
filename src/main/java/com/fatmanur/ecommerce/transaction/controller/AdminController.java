package com.fatmanur.ecommerce.transaction.controller;

import com.fatmanur.ecommerce.transaction.dto.TransactionResponse;
import com.fatmanur.ecommerce.transaction.entity.Transaction;
import com.fatmanur.ecommerce.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.base-uri}/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/orders/{orderId}/payments")
    public List<TransactionResponse> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<Transaction> transactions = transactionRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
        return transactions.stream()
                .map(t -> new TransactionResponse(
                        t.getId(),
                        t.getOrder().getId(),
                        t.getAmount(),
                        t.getStatus(),
                        t.getPaymentReference(),
                        t.getCreatedAt(),
                        t.getUpdatedAt()
                ))
                .toList();
    }
}