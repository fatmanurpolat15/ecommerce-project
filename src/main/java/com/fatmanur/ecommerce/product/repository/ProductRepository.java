package com.fatmanur.ecommerce.product.repository;

import com.fatmanur.ecommerce.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
