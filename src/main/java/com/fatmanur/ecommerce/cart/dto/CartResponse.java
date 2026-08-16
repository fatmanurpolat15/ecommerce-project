package com.fatmanur.ecommerce.cart.dto;

import java.util.Map;

public record CartResponse(
    Map<Long, CartItem> items,
    double totalPrice
) {}
