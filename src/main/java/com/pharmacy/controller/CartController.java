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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Cart", description = "Shopping cart operations")
@SecurityRequirement(name = "Bearer Authentication")
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

    @GetMapping("/{pharmacyId}")
    @Operation(
            summary = "Get cart",
            description = "Get shopping cart for a specific pharmacy"
    )
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "Pharmacy ID") @PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();

        pharmacyService.validatePharmacyActive(pharmacyId);

        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseGet(() -> {
                    User customer = userService.getById(customerId);
                    Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
                    return cartService.getOrCreateCart(customer, pharmacy);
                });

        return ResponseEntity.ok(cartMapper.toResponse(cart));
    }

    @PostMapping("/{pharmacyId}/items")
    @Operation(
            summary = "Add item to cart",
            description = "Add a product to the shopping cart"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item added"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Product not found or insufficient stock")
    })
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @PathVariable Long pharmacyId,
            @Valid @RequestBody CartItemAddRequest request) {

        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        pharmacyService.validatePharmacyActive(pharmacyId);

        Product product = productService.getById(request.getProductId());
        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new BadRequestException("Product does not belong to this pharmacy");
        }

        User customer = userService.getById(customerId);
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
        Cart cart = cartService.getOrCreateCart(customer, pharmacy);

        CartItem item = cartService.addToCart(cart, product, request.getQuantity());

        cart = cartService.getById(cart.getId());

        auditLogService.logCartItemAdded(
                pharmacyId, customerId, customerEmail,
                cart.getId(), product.getId(), product.getName(), request.getQuantity()
        );

        log.info("Item added to cart: {} x {} by customer: {}",
                request.getQuantity(), product.getName(), customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Item added to cart", cartMapper.toResponse(cart)));
    }

    @PutMapping("/{pharmacyId}/items/{itemId}")
    @Operation(
            summary = "Update cart item",
            description = "Update quantity of an item in cart (set to 0 to remove)"
    )
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long pharmacyId,
            @Parameter(description = "Cart item ID") @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {

        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        pharmacyService.validatePharmacyActive(pharmacyId);

        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "pharmacyId", pharmacyId.toString()));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        String productName = item.getProduct().getName();
        Integer oldQuantity = item.getQuantity();

        if (request.getQuantity() == 0) {
            cartService.removeFromCart(itemId);
            auditLogService.logCartItemRemoved(pharmacyId, customerId, customerEmail,
                    cart.getId(), item.getProduct().getId(), productName);
        } else {
            cartService.updateQuantity(itemId, request.getQuantity());
            auditLogService.logCartItemUpdated(pharmacyId, customerId, customerEmail,
                    cart.getId(), item.getProduct().getId(), productName, oldQuantity, request.getQuantity());
        }

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

    @DeleteMapping("/{pharmacyId}/items/{itemId}")
    @Operation(
            summary = "Remove item from cart",
            description = "Remove a product from the shopping cart"
    )
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long pharmacyId,
            @PathVariable Long itemId) {

        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        pharmacyService.validatePharmacyActive(pharmacyId);

        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "pharmacyId", pharmacyId.toString()));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", itemId));

        String productName = item.getProduct().getName();
        Long productId = item.getProduct().getId();

        cartService.removeFromCart(itemId);

        auditLogService.logCartItemRemoved(pharmacyId, customerId, customerEmail, cart.getId(), productId, productName);

        cart = cartService.getCart(customerId, pharmacyId)
                .orElseGet(() -> {
                    User customer = userService.getById(customerId);
                    Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
                    return cartService.getOrCreateCart(customer, pharmacy);
                });

        log.info("Item removed from cart: {} by customer: {}", productName, customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cartMapper.toResponse(cart)));
    }

    @DeleteMapping("/{pharmacyId}")
    @Operation(
            summary = "Clear cart",
            description = "Remove all items from the shopping cart"
    )
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "pharmacyId", pharmacyId.toString()));

        cartService.clearCart(cart.getId());

        auditLogService.logCartCleared(pharmacyId, customerId, customerEmail, cart.getId(), "User cleared cart");

        User customer = userService.getById(customerId);
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);
        cart = cartService.getOrCreateCart(customer, pharmacy);

        log.info("Cart cleared by customer: {}", customerEmail);

        return ResponseEntity.ok(ApiResponse.success("Cart cleared", cartMapper.toResponse(cart)));
    }

    @GetMapping("/{pharmacyId}/count")
    @Operation(
            summary = "Get cart item count",
            description = "Get number of items in cart (for header badge)"
    )
    public ResponseEntity<Map<String, Integer>> getCartItemCount(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();

        int count = cartService.getCart(customerId, pharmacyId)
                .map(cart -> cartService.getCartItemCount(cart.getId()))
                .orElse(0);

        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/{pharmacyId}/validate")
    @Operation(
            summary = "Validate cart",
            description = "Check if cart is valid for checkout (stock availability)"
    )
    public ResponseEntity<ApiResponse<CartResponse>> validateCart(@PathVariable Long pharmacyId) {
        Long customerId = getCurrentCustomerId();

        pharmacyService.validatePharmacyActive(pharmacyId);

        Cart cart = cartService.getCart(customerId, pharmacyId)
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

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