package com.fatmanur.ecommerce.transaction.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResult(
    Long orderId,
    String status,
    String paymentReference,
    String reason,
    BigDecimal amount,
    LocalDateTime processedAt
) {}
