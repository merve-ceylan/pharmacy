package com.pharmacy.controller;

import com.pharmacy.dto.request.CartItemAddRequest;
import com.pharmacy.dto.request.CartItemUpdateRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.CartResponse;
import com.pharmacy.entity.Cart;
import com.pharmacy.entity.CartItem;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.entity.Product;
import com.pharmacy.entity.User;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.mapper.CartMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.CartService;
import com.pharmacy.service.PharmacyService;
import com.pharmacy.service.ProductService;
import com.pharmacy.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final ProductService productService;
    private final PharmacyService pharmacyService;
    private final UserService userService;
    private final CartMapper cartMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public CartController(CartService cartService,
                          ProductService productService,
                          PharmacyService pharmacyService,
                          UserService userService,
                          CartMapper cartMapper,
                          SecurityUtils securityUtils,
                          AuditLogService auditLogService) {
        this.cartService = cartService;
        this.productService = productService;
        this.pharmacyService = pharmacyService;
        this.userService = userService;
        this.cartMapper = cartMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    /**
     * Get cart for a pharmacy
     * GET /api/customer/cart/{pharmacyId}
     */
    @GetMapping("/{pharmacyId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();

        // Validate pharmacy exists and is active
        pharmacyService.validatePharmacyActive(pharmacyId);

        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseGet(() -> {
                    // Return empty cart response if no cart exists
                    User customer = userService.getById(customerId);
                    Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
                    return cartService.getOrCreateCart(customer, pharmacy);
                });

        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    /**
     * Add item to cart
     * POST /api/customer/cart/{pharmacyId}/items
     */
    @PostMapping("/{pharmacyId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable Long pharmacyId,
            @Valid @RequestBody CartItemAddRequest request) {

        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        // Validate pharmacy
        pharmacyService.validatePharmacyActive(pharmacyId);

        // Validate product
        Product product = productService.getById(request.getProductId());
        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new BadRequestException("Product does not belong to this pharmacy");
        }

        // Get or create cart
        User customer = userService.getById(customerId);
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
        Cart cart = cartService.getOrCreateCart(customer, pharmacy);

        // Add item
        CartItem item = cartService.addToCart(cart, product, request.getQuantity());

        // Refresh cart
        cart = cartService.getById(cart.getId());

        // Audit log
        auditLogService.logCartItemAdded(
                pharmacyId, customerId, customerEmail,
                cart.getId(), product.getId(), product.getName(), request.getQuantity()
        );

        log.info("Item added to cart: {} x {} by customer: {}",
                request.getQuantity(), product.getName(), customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartMapper.toResponse(cart)));
    }

    /**
     * Update cart item quantity
     * PUT /api/customer/cart/{pharmacyId}/items/{itemId}
     */
    @PutMapping("/{pharmacyId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long pharmacyId,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {

        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        // Validate pharmacy
        pharmacyService.validatePharmacyActive(pharmacyId);

        // Get cart and validate ownership
        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "pharmacyId", pharmacyId.toString()));

        // Validate item belongs to cart
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        String productName = item.getProduct().getName();
        Integer oldQuantity = item.getQuantity();

        // Update quantity (0 means remove)
        if (request.getQuantity() == 0) {
            cartService.removeFromCart(itemId);
            auditLogService.logCartItemRemoved(pharmacyId, customerId, customerEmail,
                    cart.getId(), item.getProduct().getId(), productName);
        } else {
            cartService.updateQuantity(itemId, request.getQuantity());
            auditLogService.logCartItemUpdated(pharmacyId, customerId, customerEmail,
                    cart.getId(), item.getProduct().getId(), productName, oldQuantity, request.getQuantity());
        }

        // Refresh cart
        cart = cartService.getCart(customerId, pharmacyId)
                .orElseGet(() -> {
                    User customer = userService.getById(customerId);
                    Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
                    return cartService.getOrCreateCart(customer, pharmacy);
                });

        log.info("Cart item updated: {} quantity {} -> {} by customer: {}",
                productName, oldQuantity, request.getQuantity(), customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Cart updated", cartMapper.toResponse(cart)));
    }

    /**
     * Remove item from cart
     * DELETE /api/customer/cart/{pharmacyId}/items/{itemId}
     */
    @DeleteMapping("/{pharmacyId}/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long pharmacyId,
            @PathVariable Long itemId) {

        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        // Validate pharmacy
        pharmacyService.validatePharmacyActive(pharmacyId);

        // Get cart and validate ownership
        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "pharmacyId", pharmacyId.toString()));

        // Validate item belongs to cart
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        String productName = item.getProduct().getName();
        Long productId = item.getProduct().getId();

        // Remove item
        cartService.removeFromCart(itemId);

        // Audit log
        auditLogService.logCartItemRemoved(pharmacyId, customerId, customerEmail, cart.getId(), productId, productName);

        // Refresh cart
        cart = cartService.getCart(customerId, pharmacyId)
                .orElseGet(() -> {
                    User customer = userService.getById(customerId);
                    Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
                    return cartService.getOrCreateCart(customer, pharmacy);
                });

        log.info("Item removed from cart: {} by customer: {}", productName, customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartMapper.toResponse(cart)));
    }

    /**
     * Clear entire cart
     * DELETE /api/customer/cart/{pharmacyId}
     */
    @DeleteMapping("/{pharmacyId}")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        // Get cart
        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "pharmacyId", pharmacyId.toString()));

        // Clear cart
        cartService.clearCart(cart.getId());

        // Audit log
        auditLogService.logCartCleared(pharmacyId, customerId, customerEmail, cart.getId(), "User cleared cart");

        // Return empty cart
        User customer = userService.getById(customerId);
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
        cart = cartService.getOrCreateCart(customer, pharmacy);

        log.info("Cart cleared by customer: {}", customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cartMapper.toResponse(cart)));
    }

    /**
     * Get cart item count (for header badge)
     * GET /api/customer/cart/{pharmacyId}/count
     */
    @GetMapping("/{pharmacyId}/count")
    public ResponseEntity<Map<String, Integer>> getCartItemCount(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();

        int count = cartService.getCart(customerId, pharmacyId)
                .map(cart -> cartService.getCartItemCount(cart.getId()))
                .orElse(0);

        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Validate cart before checkout
     * GET /api/customer/cart/{pharmacyId}/validate
     */
    @GetMapping("/{pharmacyId}/validate")
    public ResponseEntity<ApiResponse<CartResponse>> validateCart(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();

        // Validate pharmacy
        pharmacyService.validatePharmacyActive(pharmacyId);

        // Get cart
        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        // Check for unavailable items
        var unavailableItems = cartService.getUnavailableItems(cart.getId());

        CartResponse response = cartMapper.toResponse(cart);

        if (!unavailableItems.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Some items are unavailable or out of stock"));
        }

        if (cart.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Cart is empty"));
        }

        return ResponseEntity.ok(ApiResponse.success("Cart is valid for checkout", response));
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentCustomerId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
    }
}