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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Products", description = "Product management endpoints")
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

    @GetMapping("/public/pharmacies/{pharmacyId}/products")
    @Operation(
            summary = "List pharmacy products",
            description = "Get paginated list of products for a pharmacy (public storefront)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Pharmacy not active")
    })
    public ResponseEntity<PageResponse<ProductResponse>> getPublicProducts(
            @Parameter(description = "Pharmacy ID") @PathVariable Long pharmacyId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir) {

        pharmacyService.validatePharmacyActive(pharmacyId);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products = productService.findByPharmacy(pharmacyId, pageable);
        Page<ProductResponse> responsePage = products.map(productMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/public/pharmacies/{pharmacyId}/products/slug/{slug}")
    @Operation(
            summary = "Get product by slug",
            description = "Get a single product by its URL-friendly slug"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
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

    @GetMapping("/public/pharmacies/{pharmacyId}/products/category/{categoryId}")
    @Operation(
            summary = "Get products by category",
            description = "List all products in a specific category"
    )
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

    @GetMapping("/public/pharmacies/{pharmacyId}/products/featured")
    @Operation(
            summary = "Get featured products",
            description = "List featured products for the pharmacy homepage"
    )
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts(@PathVariable Long pharmacyId) {
        pharmacyService.validatePharmacyActive(pharmacyId);

        List<Product> products = productService.findFeaturedProducts(pharmacyId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/public/pharmacies/{pharmacyId}/products/search")
    @Operation(
            summary = "Search products",
            description = "Search products by name or description"
    )
    public ResponseEntity<PageResponse<ProductResponse>> searchProducts(
            @PathVariable Long pharmacyId,
            @Parameter(description = "Search keyword (min 2 chars)") @RequestParam("q") String keyword,
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

    // ==================== STAFF ENDPOINTS ====================

    @GetMapping("/staff/products")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "List all products (Staff)",
            description = "Get all products for the pharmacy (admin view)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PageResponse<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Filter: ALL, ACTIVE, PASSIVE, LOW_STOCK")
            @RequestParam(required = false) String filter) {

        Long pharmacyId = getCurrentPharmacyId();

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Product> products;

        if (filter != null) {
            switch (filter) {
                case "ACTIVE":
                    products = productService.findByPharmacyAndActive(pharmacyId, true, pageable);
                    break;
                case "PASSIVE":
                    products = productService.findByPharmacyAndActive(pharmacyId, false, pageable);
                    break;
                case "LOW_STOCK":
                    products = productService.findLowStockProductsPaginated(pharmacyId, pageable);
                    break;
                default:
                    products = productService.findByPharmacy(pharmacyId, pageable);
            }
        } else {
            products = productService.findByPharmacy(pharmacyId, pageable);
        }

        Page<ProductResponse> responsePage = products.map(productMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/staff/products/{id}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get product by ID",
            description = "Get a single product by its ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        Long pharmacyId = getCurrentPharmacyId();

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @PostMapping("/staff/products")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Create product",
            description = "Create a new product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "SKU already exists")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
        Category category = categoryService.getById(request.getCategoryId());

        Product product = productMapper.toEntity(request);
        product = productService.createProduct(product, pharmacy, category);

        auditLogService.logProductCreated(pharmacyId, userId, userEmail,
                product.getId(), product.getName(), product.getSku());

        log.info("Product created: {} by user: {}", product.getName(), userEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", productMapper.toResponse(product)));
    }

    @PutMapping("/staff/products/{id}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Update product",
            description = "Update an existing product",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        if (request.getCategoryId() != null &&
                !request.getCategoryId().equals(product.getCategory().getId())) {
            Category newCategory = categoryService.getById(request.getCategoryId());
            product.setCategory(newCategory);
        }

        productMapper.updateEntity(product, request);
        product = productService.updateProduct(product);

        auditLogService.logProductUpdated(pharmacyId, userId, userEmail,
                product.getId(), product.getName(), "Product details updated");

        log.info("Product updated: {} by user: {}", product.getName(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productMapper.toResponse(product)));
    }

    @PatchMapping("/staff/products/{id}/stock")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Update stock",
            description = "Update product stock quantity",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
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

        auditLogService.logProductStockChanged(pharmacyId, userId, userEmail,
                product.getId(), product.getName(), oldStock, request.getQuantity(),
                request.getReason() != null ? request.getReason() : "Manual update");

        log.info("Stock updated for product: {} from {} to {} by user: {}",
                product.getName(), oldStock, request.getQuantity(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", productMapper.toResponse(product)));
    }

    @PatchMapping("/staff/products/{id}/featured")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Toggle featured status",
            description = "Mark or unmark a product as featured",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ProductResponse>> toggleFeatured(
            @PathVariable Long id,
            @Parameter(description = "Featured status") @RequestParam boolean featured) {

        Long pharmacyId = getCurrentPharmacyId();

        Product product = productService.getById(id);
        validateProductBelongsToPharmacy(product, pharmacyId);

        product = productService.setFeatured(id, featured);

        String message = featured ? "Product marked as featured" : "Product removed from featured";
        return ResponseEntity.ok(ApiResponse.success(message, productMapper.toResponse(product)));
    }

    @PatchMapping("/staff/products/{id}/activate")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Activate product",
            description = "Activate a product to make it visible in storefront",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
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

    @PatchMapping("/staff/products/{id}/deactivate")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Deactivate product",
            description = "Deactivate a product to hide it from storefront",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
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

    @GetMapping("/staff/products/low-stock")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get low stock products",
            description = "List products with stock below threshold",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<ProductResponse>> getLowStockProducts() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Product> products = productService.findLowStockProducts(pharmacyId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/staff/products/out-of-stock")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get out of stock products",
            description = "List products with zero stock",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<ProductResponse>> getOutOfStockProducts() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Product> products = productService.findOutOfStockProducts(pharmacyId);
        List<ProductResponse> responses = products.stream()
                .map(productMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/staff/products/count")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get product count",
            description = "Get total number of products",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
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

    @GetMapping("/staff/products/stats")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get product statistics",
            description = "Get product statistics for the pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> getProductStats() {
        Long pharmacyId = getCurrentPharmacyId();

        Map<String, Object> stats = new HashMap<>();

        // Total products
        long total = productService.countByPharmacy(pharmacyId);

        // Active products
        long active = productService.countActiveByPharmacy(pharmacyId);

        // Inactive products
        long inactive = total - active;

        stats.put("total", total);
        stats.put("active", active);
        stats.put("inactive", inactive);

        // Low stock products
        List<Product> lowStockProducts = productService.findLowStockProducts(pharmacyId);
        stats.put("lowStock", lowStockProducts.size());

        return ResponseEntity.ok(stats);
    }
}