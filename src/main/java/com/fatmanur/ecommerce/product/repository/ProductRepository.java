package com.fatmanur.ecommerce.product.repository;

import com.fatmanur.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByIdAndDeletedFalse(Long id);

    Page<Product> findAllByDeletedFalse(Pageable pageable);

    Page<Product> findAllByCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);

    Page<Product> findAllByNameContainingIgnoreCaseAndDeletedFalse(String keyword, Pageable pageable);

    Page<Product> findAllByPriceBetweenAndDeletedFalse(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
