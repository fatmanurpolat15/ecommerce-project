package com.fatmanur.ecommerce.stock.service;

import com.fatmanur.ecommerce.product.entity.Product;
import com.fatmanur.ecommerce.product.exception.ProductNotFoundException;
import com.fatmanur.ecommerce.product.repository.ProductRepository;
import com.fatmanur.ecommerce.stock.dto.StockRequest;
import com.fatmanur.ecommerce.stock.dto.StockResponse;
import com.fatmanur.ecommerce.stock.entity.Inventory;
import com.fatmanur.ecommerce.stock.exception.StockNotFoundException;
import com.fatmanur.ecommerce.stock.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StockService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Cacheable(value = "stocks", key = "#productId")
    public StockResponse getByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new StockNotFoundException("Stock not found for product: " + productId));
        return toResponse(inventory);
    }

    @CacheEvict(value = "stocks", key = "#productId")
    public StockResponse adjust(Long productId, StockRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> Inventory.builder()
                        .product(product)
                        .availableQuantity(0)
                        .reservedQuantity(0)
                        .build());

        inventory.setAvailableQuantity(request.quantity());
        inventoryRepository.save(inventory);

        return toResponse(inventory);
    }

    private StockResponse toResponse(Inventory inventory) {
        return new StockResponse(
                inventory.getId(),
                inventory.getProduct().getId(),
                inventory.getAvailableQuantity(),
                inventory.getReservedQuantity()
        );
    }
}
