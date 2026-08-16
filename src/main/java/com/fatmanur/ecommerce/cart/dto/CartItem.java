package com.fatmanur.ecommerce.cart.dto;

public record CartItem (

    Long productId,
    String name,
    double price,
    int quantity

) {}
