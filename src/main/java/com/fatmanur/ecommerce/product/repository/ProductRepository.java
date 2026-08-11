package com.fatmanur.ecommerce.product.repository;

import com.fatmanur.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    Page<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Product> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String keyword, Pageable pageable);
    Page<Product> findByCategoryIdAndPriceBetween(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndPriceBetween(String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    Page<Product> findByCategoryIdAndNameContainingIgnoreCaseAndPriceBetween(Long categoryId, String keyword, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
