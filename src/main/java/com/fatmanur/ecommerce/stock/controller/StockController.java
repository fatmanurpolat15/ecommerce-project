package com.fatmanur.ecommerce.stock.controller;

import com.fatmanur.ecommerce.stock.dto.StockRequest;
import com.fatmanur.ecommerce.stock.dto.StockResponse;
import com.fatmanur.ecommerce.stock.service.StockService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@AllArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getByProductId(productId));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockResponse> adjust(@PathVariable Long productId,
                                                @Valid @RequestBody StockRequest request) {
        return ResponseEntity.ok(stockService.adjust(productId, request));
    }

}
