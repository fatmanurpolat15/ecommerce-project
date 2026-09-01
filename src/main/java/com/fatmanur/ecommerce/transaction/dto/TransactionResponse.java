package com.fatmanur.ecommerce.transaction.dto;

import com.fatmanur.ecommerce.transaction.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long orderId,
        BigDecimal amount,
        TransactionStatus status,
        String paymentReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
