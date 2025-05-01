package com.dms.document.service;

import com.dms.document.dto.CategoryRequest;
import com.dms.document.dto.CategoryResponse;
import com.dms.document.exception.DuplicateResourceException;
import com.dms.document.exception.ResourceNotFoundException;
import com.dms.document.model.DocumentCategory;
import com.dms.document.repository.DocumentCategoryRepository;
import com.dms.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final DocumentCategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category with name " + request.getName() + " already exists");
        }
        
        DocumentCategory category = DocumentCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        
        DocumentCategory savedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(savedCategory);
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::mapToCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long id) {
        DocumentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        return mapToCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        DocumentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        // Check if another category with the same name exists
        categoryRepository.findByName(request.getName())
                .ifPresent(existingCategory -> {
                    if (!existingCategory.getId().equals(id)) {
                        throw new DuplicateResourceException("Category with name " + request.getName() + " already exists");
                    }
                });
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        
        DocumentCategory updatedCategory = categoryRepository.save(category);
        return mapToCategoryResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        DocumentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        // Check if there are documents using this category
        if (!documentRepository.findByCategoryId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete category that is in use by documents");
        }
        
        categoryRepository.delete(category);
    }
    
    private CategoryResponse mapToCategoryResponse(DocumentCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}