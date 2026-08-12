package com.fatmanur.ecommerce.product.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        String name,
        String description,
        @NotNull BigDecimal price,
        String currency,
        String imageUrl,
        @NotNull Long categoryId,
        Boolean active
) {}
