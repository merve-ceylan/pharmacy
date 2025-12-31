package com.pharmacy.controller;

import com.pharmacy.dto.request.CategoryCreateRequest;
import com.pharmacy.dto.request.CategoryUpdateRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.CategoryResponse;
import com.pharmacy.entity.Category;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.mapper.CategoryMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Categories", description = "Category management endpoints")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public CategoryController(CategoryService categoryService,
                              CategoryMapper categoryMapper,
                              SecurityUtils securityUtils,
                              AuditLogService auditLogService) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/public/categories")
    @Operation(
            summary = "List all categories",
            description = "Get all active root categories with their children (for storefront menu)"
    )
    public ResponseEntity<List<CategoryResponse>> getPublicCategories() {
        List<Category> categories = categoryService.findRootCategories();
        List<CategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponseWithChildren)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/public/categories/slug/{slug}")
    @Operation(
            summary = "Get category by slug",
            description = "Get a category by its URL-friendly slug"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        Category category = categoryService.getBySlug(slug);

        if (!category.isActive()) {
            throw new BadRequestException("Category not found");
        }

        return ResponseEntity.ok(categoryMapper.toResponseWithChildren(category));
    }

    @GetMapping("/public/categories/{parentId}/subcategories")
    @Operation(
            summary = "Get subcategories",
            description = "List subcategories of a parent category"
    )
    public ResponseEntity<List<CategoryResponse>> getSubcategories(
            @Parameter(description = "Parent category ID") @PathVariable Long parentId) {
        List<Category> categories = categoryService.findSubcategories(parentId);
        List<CategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "List all categories (Admin)",
            description = "Get all categories including inactive ones",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryService.findAll();
        List<CategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get category by ID",
            description = "Get a category with its children",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return ResponseEntity.ok(categoryMapper.toResponseWithChildren(category));
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Create category",
            description = "Create a new category (optionally as subcategory)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Slug already exists")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryMapper.toEntity(request);

        if (request.getParentId() != null) {
            category = categoryService.createSubcategory(category, request.getParentId());
        } else {
            category = categoryService.createCategory(category);
        }

        auditLogService.logCategoryCreated(userId, userEmail, category.getId(), category.getName());
        log.info("Category created: {} by user: {}", category.getName(), userEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", categoryMapper.toResponse(category)));
    }

    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Update category",
            description = "Update an existing category",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryService.getById(id);

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category newParent = categoryService.getById(request.getParentId());
            category.setParent(newParent);
        }

        categoryMapper.updateEntity(category, request);
        category = categoryService.updateCategory(category);

        auditLogService.logCategoryUpdated(userId, userEmail, category.getId(), category.getName(), "Category updated");
        log.info("Category updated: {} by user: {}", category.getName(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", categoryMapper.toResponse(category)));
    }

    @PatchMapping("/admin/categories/{id}/order")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Update display order",
            description = "Change the display order of a category",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> updateDisplayOrder(
            @PathVariable Long id,
            @Parameter(description = "New display order") @RequestParam Integer order) {

        Category category = categoryService.updateDisplayOrder(id, order);
        return ResponseEntity.ok(ApiResponse.success("Display order updated", categoryMapper.toResponse(category)));
    }

    @PatchMapping("/admin/categories/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Activate category",
            description = "Activate a category to make it visible",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> activateCategory(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryService.activateCategory(id);

        auditLogService.logCategoryUpdated(userId, userEmail, category.getId(), category.getName(), "Category activated");

        return ResponseEntity.ok(ApiResponse.success("Category activated", categoryMapper.toResponse(category)));
    }

    @PatchMapping("/admin/categories/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Deactivate category",
            description = "Deactivate a category (also deactivates children)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivateCategory(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryService.deactivateCategory(id);

        auditLogService.logCategoryUpdated(userId, userEmail, category.getId(), category.getName(), "Category deactivated");

        return ResponseEntity.ok(ApiResponse.success("Category deactivated", categoryMapper.toResponse(category)));
    }

    @GetMapping("/admin/categories/check-slug")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Check slug availability",
            description = "Check if a slug is available for use",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Boolean>> checkSlugAvailability(
            @Parameter(description = "Slug to check") @RequestParam String slug) {
        boolean available = categoryService.isSlugAvailable(slug);
        return ResponseEntity.ok(ApiResponse.success("Slug availability checked", available));
    }
}