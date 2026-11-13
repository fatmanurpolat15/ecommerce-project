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

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;


import java.math.BigDecimal;
import java.util.List;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductResponse create(ProductRequest request) {
        var category = categoryRepository.findByIdAndDeletedFalse(request.categoryId())
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


    @Cacheable(value = "products" , key = "#id")
    public ProductResponse getById(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        return toResponse(product);
    }

    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAllByDeletedFalse(pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getAll(List<Long> categoryIds, Long categoryId, Pageable pageable) {
        List<Long> targetCategoryIds = resolveCategoryIds(categoryIds, categoryId);
        if (targetCategoryIds != null) {
            return productRepository.findAllByCategoryIdInAndDeletedFalse(targetCategoryIds, pageable).map(this::toResponse);
        }
        return getAll(pageable);
    }

    public Page<ProductResponse> getByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findAllByCategoryIdAndDeletedFalse(categoryId, pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getByCategoryIds(List<Long> categoryIds, Pageable pageable) {
        return getAll(categoryIds, null, pageable);
    }

    public Page<ProductResponse> searchByKeyword(String keyword, Pageable pageable) {
        return productRepository.findAllByNameContainingIgnoreCaseAndDeletedFalse(keyword, pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, List<Long> categoryIds, Long categoryId, Pageable pageable) {
        List<Long> targetCategoryIds = resolveCategoryIds(categoryIds, categoryId);
        if (targetCategoryIds != null) {
            return productRepository.findAllByCategoryIdInAndPriceBetweenAndDeletedFalse(targetCategoryIds, minPrice, maxPrice, pageable).map(this::toResponse);
        }
        return productRepository.findAllByPriceBetweenAndDeletedFalse(minPrice, maxPrice, pageable).map(this::toResponse);
    }

    public Page<ProductResponse> getByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, List<Long> categoryIds, Pageable pageable) {
        return getByPriceRange(minPrice, maxPrice, categoryIds, null, pageable);
    }

    private List<Long> resolveCategoryIds(List<Long> categoryIds, Long categoryId) {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            return categoryIds;
        }
        if (categoryId != null) {
            return List.of(categoryId);
        }
        return null;
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));

        var category = categoryRepository.findByIdAndDeletedFalse(request.categoryId())
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

    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        product.setDeleted(true);
        productRepository.save(product);
    }

    @CacheEvict(value = "products", key = "#id")
    public ProductResponse toggleActive(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        product.setActive(!product.isActive());
        productRepository.save(product);
        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getImageUrl(),
                product.getCategory().getName(),
                product.isActive());
    }
}
