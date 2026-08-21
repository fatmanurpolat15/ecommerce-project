package com.fatmanur.ecommerce.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    String status,
    String shippingAddress,
    List<OrderItemResponse> items,
    BigDecimal totalPrice,
    LocalDateTime createdAt,
    List<StatusTimeLineItem> timeline
) {

    public record OrderItemResponse(
        String productName,
        BigDecimal price,
        int quantity,
        BigDecimal lineTotal
    ) {}

    public record StatusTimeLineItem(
            String fromStatus,
            String toStatus,
            LocalDateTime changedAt

    ){}
}
