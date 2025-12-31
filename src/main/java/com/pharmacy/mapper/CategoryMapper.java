package com.pharmacy.mapper;

import com.pharmacy.dto.request.CategoryCreateRequest;
import com.pharmacy.dto.request.CategoryUpdateRequest;
import com.pharmacy.dto.response.CategoryResponse;
import com.pharmacy.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryCreateRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setImageUrl(request.getImageUrl());
        category.setActive(request.getActive() != null ? request.getActive() : true);
        return category;
    }

    public void updateEntity(Category category, CategoryUpdateRequest request) {
        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }
    }

    public CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSlug(category.getSlug());
        response.setDescription(category.getDescription());
        response.setDisplayOrder(category.getDisplayOrder());
        response.setImageUrl(category.getImageUrl());
        response.setActive(category.isActive());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());

        if (category.getParent() != null) {
            response.setParentId(category.getParent().getId());
            response.setParentName(category.getParent().getName());
        }

        return response;
    }

    public CategoryResponse toResponseWithChildren(Category category) {
        CategoryResponse response = toResponse(category);

        if (category.getChildren() != null && !category.getChildren().isEmpty()) {
            List<CategoryResponse> children = category.getChildren().stream()
                    .filter(Category::isActive)
                    .map(this::toResponse)
                    .toList();
            response.setChildren(children);
        }

        return response;
    }

    public CategoryResponse toResponseWithProductCount(Category category, int productCount) {
        CategoryResponse response = toResponse(category);
        response.setProductCount(productCount);
        return response;
    }
}