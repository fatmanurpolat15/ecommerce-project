package com.fatmanur.ecommerce.stock.dto;

import jakarta.validation.constraints.Min;

public record StockRequest(
        @Min(0) int quantity
) {}