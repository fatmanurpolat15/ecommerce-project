package com.fatmanur.ecommerce.product.service;

import com.fatmanur.ecommerce.category.exception.CategoryNotFoundException;
import com.fatmanur.ecommerce.category.repository.CategoryRepository;
import com.fatmanur.ecommerce.product.dto.ProductRequest;
import com.fatmanur.ecommerce.product.dto.ProductResponse;
import com.fatmanur.ecommerce.product.entity.Product;
import com.fatmanur.ecommerce.product.exception.ProductNotFoundException;
import com.fatmanur.ecommerce.product.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponse create(ProductRequest request) {
        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "TRY")
                .imageUrl(request.imageUrl())
                .category(category)
                .build();
        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public Page<ProductResponse> getAll(Pageable pageable, Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice) {
        if (categoryId != null && keyword != null && minPrice != null && maxPrice != null) {
            return productRepository.findByCategoryIdAndNameContainingIgnoreCaseAndPriceBetween(categoryId, keyword, minPrice, maxPrice, pageable).map(this::toResponse);
        } else if (categoryId != null && keyword != null) {
            return productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, keyword, pageable).map(this::toResponse);
        } else if (categoryId != null && minPrice != null && maxPrice != null) {
            return productRepository.findByCategoryIdAndPriceBetween(categoryId, minPrice, maxPrice, pageable).map(this::toResponse);
        } else if (keyword != null && minPrice != null && maxPrice != null) {
            return productRepository.findByNameContainingIgnoreCaseAndPriceBetween(keyword, minPrice, maxPrice, pageable).map(this::toResponse);
        } else if (categoryId != null) {
            return productRepository.findByCategoryId(categoryId, pageable).map(this::toResponse);
        } else if (keyword != null) {
            return productRepository.findByNameContainingIgnoreCase(keyword, pageable).map(this::toResponse);
        } else if (minPrice != null && maxPrice != null) {
            return productRepository.findByPriceBetween(minPrice, maxPrice, pageable).map(this::toResponse);
        }
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return toResponse(product);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        var category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCurrency(request.currency());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getImageUrl(),
                product.getCategory().getName());
    }
}
