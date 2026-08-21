package com.fatmanur.ecommerce.order.dto;

import com.fatmanur.ecommerce.order.enums.OrderStatus;

public record OrderStatusUpdateRequest(
    OrderStatus status
) { }
