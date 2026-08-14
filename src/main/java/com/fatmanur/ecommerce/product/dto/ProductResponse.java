package com.fatmanur.ecommerce.product.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String imageUrl,
        String categoryName,
        Boolean active
) implements Serializable {}
