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

    /**
     * Get all active root categories with children (for storefront menu)
     * GET /api/public/categories
     */
    @GetMapping("/public/categories")
    public ResponseEntity<List<CategoryResponse>> getPublicCategories() {
        List<Category> categories = categoryService.findRootCategories();
        List<CategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponseWithChildren)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get single category by slug
     * GET /api/public/categories/slug/{slug}
     */
    @GetMapping("/public/categories/slug/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        Category category = categoryService.getBySlug(slug);

        if (!category.isActive()) {
            throw new BadRequestException("Category not found");
        }

        return ResponseEntity.ok(categoryMapper.toResponseWithChildren(category));
    }

    /**
     * Get subcategories of a parent
     * GET /api/public/categories/{parentId}/subcategories
     */
    @GetMapping("/public/categories/{parentId}/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubcategories(@PathVariable Long parentId) {
        List<Category> categories = categoryService.findSubcategories(parentId);
        List<CategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // ==================== ADMIN ENDPOINTS (Super Admin Only) ====================

    /**
     * Get all categories (including inactive)
     * GET /api/admin/categories
     */
    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryService.findAll();
        List<CategoryResponse> responses = categories.stream()
                .map(categoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get single category by ID
     * GET /api/admin/categories/{id}
     */
    @GetMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        return ResponseEntity.ok(categoryMapper.toResponseWithChildren(category));
    }

    /**
     * Create new category
     * POST /api/admin/categories
     */
    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request) {

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryMapper.toEntity(request);

        // Handle parent category
        if (request.getParentId() != null) {
            category = categoryService.createSubcategory(category, request.getParentId());
        } else {
            category = categoryService.createCategory(category);
        }

        // Audit log
        auditLogService.logCategoryCreated(userId, userEmail, category.getId(), category.getName());
        log.info("Category created: {} by user: {}", category.getName(), userEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", categoryMapper.toResponse(category)));
    }

    /**
     * Update category
     * PUT /api/admin/categories/{id}
     */
    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryUpdateRequest request) {

        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryService.getById(id);

        // Handle parent change
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }
            Category newParent = categoryService.getById(request.getParentId());
            category.setParent(newParent);
        }

        categoryMapper.updateEntity(category, request);
        category = categoryService.updateCategory(category);

        // Audit log
        auditLogService.logCategoryUpdated(userId, userEmail, category.getId(), category.getName(), "Category updated");
        log.info("Category updated: {} by user: {}", category.getName(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", categoryMapper.toResponse(category)));
    }

    /**
     * Update display order
     * PATCH /api/admin/categories/{id}/order
     */
    @PatchMapping("/admin/categories/{id}/order")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateDisplayOrder(
            @PathVariable Long id,
            @RequestParam Integer order) {

        Category category = categoryService.updateDisplayOrder(id, order);
        return ResponseEntity.ok(ApiResponse.success("Display order updated", categoryMapper.toResponse(category)));
    }

    /**
     * Activate category
     * PATCH /api/admin/categories/{id}/activate
     */
    @PatchMapping("/admin/categories/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> activateCategory(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryService.activateCategory(id);

        auditLogService.logCategoryUpdated(userId, userEmail, category.getId(), category.getName(), "Category activated");

        return ResponseEntity.ok(ApiResponse.success("Category activated", categoryMapper.toResponse(category)));
    }

    /**
     * Deactivate category (also deactivates children)
     * PATCH /api/admin/categories/{id}/deactivate
     */
    @PatchMapping("/admin/categories/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> deactivateCategory(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Category category = categoryService.deactivateCategory(id);

        auditLogService.logCategoryUpdated(userId, userEmail, category.getId(), category.getName(), "Category deactivated");

        return ResponseEntity.ok(ApiResponse.success("Category deactivated", categoryMapper.toResponse(category)));
    }

    /**
     * Check if slug is available
     * GET /api/admin/categories/check-slug?slug=xxx
     */
    @GetMapping("/admin/categories/check-slug")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> checkSlugAvailability(@RequestParam String slug) {
        boolean available = categoryService.isSlugAvailable(slug);
        return ResponseEntity.ok(ApiResponse.success("Slug availability checked", available));
    }
}
