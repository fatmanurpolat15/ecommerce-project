package com.fatmanur.ecommerce.stock.dto;

import java.io.Serializable;

public record StockResponse(
        Long id,
        Long productId,
        int availableQuantity,
        int reservedQuantity
) implements Serializable {}