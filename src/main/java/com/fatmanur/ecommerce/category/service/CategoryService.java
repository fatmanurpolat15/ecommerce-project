package com.fatmanur.ecommerce.category.service;

import com.fatmanur.ecommerce.category.dto.CategoryRequest;
import com.fatmanur.ecommerce.category.dto.CategoryResponse;
import com.fatmanur.ecommerce.category.entity.Category;
import com.fatmanur.ecommerce.category.exception.CategoryAlreadyExistsException;
import com.fatmanur.ecommerce.category.exception.CategoryNotFoundException;
import com.fatmanur.ecommerce.category.repository.CategoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new CategoryAlreadyExistsException("Category name already exists");
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Cacheable(value = "categories")
    public List<CategoryResponse> getAll() {
        return categoryRepository.findByDeletedFalse().stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        return toResponse(category);
    }

    @CacheEvict(value = "categories", key = "#id")
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        category.setName(request.name());
        category.setDescription(request.description());
        Category updated = categoryRepository.save(category);
        return toResponse(updated);
    }

    @CacheEvict(value = "categories", key = "#id")
    public void delete(Long id) {
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found"));
        category.setDeleted(true);
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
