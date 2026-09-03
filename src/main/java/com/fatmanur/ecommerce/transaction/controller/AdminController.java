package com.fatmanur.ecommerce.transaction.controller;

import com.fatmanur.ecommerce.transaction.dto.TransactionResponse;
import com.fatmanur.ecommerce.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${api.base-uri}/admin")
@RequiredArgsConstructor
public class AdminController {

    private final TransactionService transactionService;

    @GetMapping("/orders/{orderId}/payments")
    public List<TransactionResponse> getPaymentsByOrderId(@PathVariable Long orderId) {
        return transactionService.getTransactionsByOrderId(orderId);
    }

    @GetMapping("/payments")
    public List<TransactionResponse> getAllPayments() {
        return transactionService.getAllTransactions();
    }
}
