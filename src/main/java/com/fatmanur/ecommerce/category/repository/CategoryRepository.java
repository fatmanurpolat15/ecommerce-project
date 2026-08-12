package com.fatmanur.ecommerce.category.repository;

import com.fatmanur.ecommerce.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByIdAndDeletedFalse(Long id);

    List<Category> findByDeletedFalse();

    boolean existsByName(String name);
}
