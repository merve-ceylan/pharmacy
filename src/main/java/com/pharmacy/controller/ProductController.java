package com.pharmacy.controller;

import com.pharmacy.dto.request.ProductCreateRequest;
import com.pharmacy.dto.request.ProductUpdateRequest;
import com.pharmacy.dto.request.StockUpdateRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.PageResponse;
import com.pharmacy.dto.response.ProductResponse;
import com.pharmacy.entity.Category;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.entity.Product;
import com.pharmacy.exception.AccessDeniedException;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.mapper.ProductMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.CategoryService;
import com.pharmacy.service.PharmacyService;
import com.pharmacy.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final CategoryService categoryService;
    private final PharmacyService pharmacyService;
    private final ProductMapper productMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public ProductController(ProductService productService,
                             CategoryService categoryService,
                             PharmacyService pharmacyService,
                             ProductMapper productMapper,
                             SecurityUtils securityUtils,
                             AuditLogService auditLogService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.pharmacyService = pharmacyService;
        this.productMapper = productMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get products for a pharmacy (public storefront)
     * GET /api/public/pharmacies/{pharmacyId}/products
     */
    @GetMapping("/public/pharmacies/{pharmacyId}/products")
    public ResponseEntity<PageResponse<ProductResponse>> getPublicProducts(
            @PathVariable Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        // Validate pharmacy exists and is active
        pharmacyService.validatePharmacyActive(pharmacyId);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productService.findByPharmacy(pharmacyId, pageable);
        Page<ProductResponse> responsePage = products.map(productMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    /**
     * Get single product by slug (public)
     * GET /api/public/pharmacies/{pharmacyId}/products/slug/{slug}
     */
    @GetMapping("/public/pharmacies/{pharmacyId}/products/slug/{slug}")
    public ResponseEntity<ProductResponse> getPublicProductBySlug(
            @PathVariable Long pharmacyId,
            @PathVariable String slug) {

        pharmacyService.validatePharmacyActive(pharmacyId);

        Product product = productService.findByPharmacyAndSlug(pharmacyId, slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));

        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product", "slug", slug);
        }

        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    /**
     * Get products by category (public)
     * GET /api/public/pharmacies/{pharmacyId}/products/category/{categoryId}
     */
    @GetMapping("/public/pharmacies/{pharmacyId}/products/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getPublicProductsByCategory(
            @PathVariable Long pharmacyId,
            @PathVariable Long categoryId) {

        pharmacyService.validatePharmacyActive(pharmacyId);

        List<Product> products = productService.findByCategory(pharmacyId, categoryId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get featured products (public)
     * GET /api/public/pharmacies/{pharmacyId}/products/featured
     */
    @GetMapping("/public/pharmacies/{pharmacyId}/products/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts(@PathVariable Long pharmacyId) {
        pharmacyService.validatePharmacyActive(pharmacyId);

        List<Product> products = productService.findFeaturedProducts(pharmacyId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Search products (public)
     * GET /api/public/pharmacies/{pharmacyId}/products/search?q=keyword
     */
    @GetMapping("/public/pharmacies/{pharmacyId}/products/search")
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @PathVariable Long pharmacyId,
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        pharmacyService.validatePharmacyActive(pharmacyId);

        if (keyword == null || keyword.trim().length() < 2) {
            throw new BadRequestException("Search keyword must be at least 2 characters");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productService.searchProducts(pharmacyId, keyword.trim(), pageable);
        Page<ProductResponse> responsePage = products.map(productMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    // ==================== STAFF ENDPOINTS (Pharmacy Owner & Staff) ====================

    /**
     * Get all products for pharmacy management
     * GET /api/staff/products
     */
    @GetMapping("/staff/products")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Long pharmacyId = getCurrentPharmacyId();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productService.findByPharmacy(pharmacyId, pageable);
        Page<ProductResponse> responsePage = products.map(productMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    /**
     * Get single product by ID
     * GET /api/staff/products/{id}
     */
    @GetMapping("/staff/products/{id}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Long pharmacyId = getCurrentPharmacyId();

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    /**
     * Create new product
     * POST /api/staff/products
     */
    @PostMapping("/staff/products")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
        Category category = categoryService.getById(request.getCategoryId());

        Product product = productMapper.toEntity(request);
        product = productService.createProduct(product, pharmacy, category);

        // Audit log
        auditLogService.logProductCreated(pharmacyId, userId, userEmail,
                product.getId(), product.getName(), product.getSku());

        log.info("Product created: {} by user: {}", product.getName(), userEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", productMapper.toResponse(product)));
    }

    /**
     * Update product
     * PUT /api/staff/products/{id}
     */
    @PutMapping("/staff/products/{id}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        // Handle category change
        if (request.getCategoryId() != null &&
                !request.getCategoryId().equals(product.getCategory().getId())) {
            Category newCategory = categoryService.getById(request.getCategoryId());
            product.setCategory(newCategory);
        }

        productMapper.updateEntity(product, request);
        product = productService.updateProduct(product);

        // Audit log
        auditLogService.logProductUpdated(pharmacyId, userId, userEmail,
                product.getId(), product.getName(), "Product details updated");

        log.info("Product updated: {} by user: {}", product.getName(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productMapper.toResponse(product)));
    }

    /**
     * Update stock quantity
     * PATCH /api/staff/products/{id}/stock
     */
    @PatchMapping("/staff/products/{id}/stock")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody StockUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        Integer oldStock = product.getStockQuantity();
        product = productService.updateStock(id, request.getQuantity());

        // Audit log
        auditLogService.logProductStockChanged(pharmacyId, userId, userEmail,
                product.getId(), product.getName(), oldStock, request.getQuantity(),
                request.getReason() != null ? request.getReason() : "Manual update");

        log.info("Stock updated for product: {} from {} to {} by user: {}",
                product.getName(), oldStock, request.getQuantity(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", productMapper.toResponse(product)));
    }

    /**
     * Toggle featured status
     * PATCH /api/staff/products/{id}/featured
     */
    @PatchMapping("/staff/products/{id}/featured")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleFeatured(
            @PathVariable Long id,
            @RequestParam boolean featured) {

        Long pharmacyId = getCurrentPharmacyId();

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        product = productService.setFeatured(id, featured);

        String message = featured ? "Product marked as featured" : "Product removed from featured";
        return ResponseEntity.ok(ApiResponse.success(message, productMapper.toResponse(product)));
    }

    /**
     * Activate product
     * PATCH /api/staff/products/{id}/activate
     */
    @PatchMapping("/staff/products/{id}/activate")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> activateProduct(@PathVariable Long id) {
        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        product = productService.activateProduct(id);

        auditLogService.logProductActivated(pharmacyId, userId, userEmail, product.getId(), product.getName());

        return ResponseEntity.ok(ApiResponse.success("Product activated", productMapper.toResponse(product)));
    }

    /**
     * Deactivate product
     * PATCH /api/staff/products/{id}/deactivate
     */
    @PatchMapping("/staff/products/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivateProduct(@PathVariable Long id) {
        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        product = productService.deactivateProduct(id);

        auditLogService.logProductDeactivated(pharmacyId, userId, userEmail, product.getId(), product.getName());

        return ResponseEntity.ok(ApiResponse.success("Product deactivated", productMapper.toResponse(product)));
    }

    /**
     * Get low stock products
     * GET /api/staff/products/low-stock
     */
    @GetMapping("/staff/products/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Product> products = productService.findLowStockProducts(pharmacyId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get out of stock products
     * GET /api/staff/products/out-of-stock
     */
    @GetMapping("/staff/products/out-of-stock")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<List<ProductResponse>> getOutOfStockProducts() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Product> products = productService.findOutOfStockProducts(pharmacyId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get product count
     * GET /api/staff/products/count
     */
    @GetMapping("/staff/products/count")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<Long>> getProductCount() {
        Long pharmacyId = getCurrentPharmacyId();
        long count = productService.countByPharmacy(pharmacyId);
        return ResponseEntity.ok(ApiResponse.success("Product count retrieved", count));
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentPharmacyId() {
        return securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));
    }

    private void validateProductBelongsToPharmacy(Product product, Long pharmacyId) {
        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw AccessDeniedException.resourceAccess("product");
        }
    }
}
