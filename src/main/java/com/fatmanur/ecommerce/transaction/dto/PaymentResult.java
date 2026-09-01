package com.fatmanur.ecommerce.transaction.dto;

import com.fatmanur.ecommerce.transaction.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
    Long orderId,
    TransactionStatus status,
    String paymentReference,
    String reason,
    BigDecimal amount,
    LocalDateTime processedAt
) {}
