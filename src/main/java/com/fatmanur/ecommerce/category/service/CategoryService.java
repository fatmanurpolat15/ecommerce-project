package com.fatmanur.ecommerce.category.service;

import com.fatmanur.ecommerce.category.dto.CategoryRequest;
import com.fatmanur.ecommerce.category.dto.CategoryResponse;
import com.fatmanur.ecommerce.category.entity.Category;
import com.fatmanur.ecommerce.category.exception.CategoryAlreadyExistsException;
import com.fatmanur.ecommerce.category.exception.CategoryNotFoundException;
import com.fatmanur.ecommerce.category.repository.CategoryRepository;
import lombok.AllArgsConstructor;
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

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
