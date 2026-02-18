package com.pharmacy.controller;

import com.pharmacy.dto.request.OrderCancelRequest;
import com.pharmacy.dto.request.OrderCreateRequest;
import com.pharmacy.dto.request.OrderStatusUpdateRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.OrderResponse;
import com.pharmacy.dto.response.PageResponse;
import com.pharmacy.entity.Cart;
import com.pharmacy.entity.Order;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.AccessDeniedException;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.mapper.OrderMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.CartService;
import com.pharmacy.service.OrderService;
import com.pharmacy.service.PharmacyService;
import com.pharmacy.service.UserService;
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
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final CartService cartService;
    private final UserService userService;
    private final PharmacyService pharmacyService;
    private final OrderMapper orderMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public OrderController(OrderService orderService,
                           CartService cartService,
                           UserService userService,
                           PharmacyService pharmacyService,
                           OrderMapper orderMapper,
                           SecurityUtils securityUtils,
                           AuditLogService auditLogService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.userService = userService;
        this.pharmacyService = pharmacyService;
        this.orderMapper = orderMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    // ==================== CUSTOMER ENDPOINTS ====================

    @PostMapping("/customer/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Create order",
            description = "Create a new order from the shopping cart",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cart is empty or validation error")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        pharmacyService.validatePharmacyActive(request.getPharmacyId());

        Cart cart = cartService.getCart(customerId, request.getPharmacyId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        Order order = orderService.createOrderFromCart(
                cart,
                request.getDeliveryType(),
                request.getShippingAddress(),
                request.getShippingCity(),
                request.getShippingDistrict(),
                request.getShippingPostalCode(),
                request.getShippingPhone(),
                request.getNotes()
        );

        auditLogService.logOrderCreated(
                request.getPharmacyId(), customerId, customerEmail,
                order.getId(), order.getOrderNumber(), order.getTotalAmount()
        );

        log.info("Order created: {} by customer: {}", order.getOrderNumber(), customerEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", orderMapper.toResponseWithItems(order)));
    }

    @GetMapping("/customer/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get my orders",
            description = "Get paginated list of orders for the current customer",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PageResponse<OrderResponse>> getCustomerOrders(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.findByCustomer(customerId, pageable);
        Page<OrderResponse> responsePage = orders.map(orderMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/customer/orders/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get order details",
            description = "Get details of a specific order",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<OrderResponse> getCustomerOrder(@PathVariable String orderNumber) {
        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        Order order = orderService.getByOrderNumber(orderNumber);

        if (!order.getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        return ResponseEntity.ok(orderMapper.toResponseWithItems(order));
    }

    @PostMapping("/customer/orders/{orderNumber}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Cancel order",
            description = "Cancel an order (only if status allows)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrderByCustomer(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderCancelRequest request) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);

        if (!order.getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        order = orderService.cancelOrder(order.getId(), request.getReason(), customerId);

        auditLogService.logOrderCancelled(
                order.getPharmacy().getId(), customerId, customerEmail,
                order.getId(), order.getOrderNumber(), request.getReason()
        );

        log.info("Order cancelled by customer: {} - {}", order.getOrderNumber(), request.getReason());

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", orderMapper.toResponse(order)));
    }

    // ==================== STAFF ENDPOINTS ====================

    @GetMapping("/staff/orders")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get pharmacy orders",
            description = "Get paginated list of orders for the pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PageResponse<OrderResponse>> getPharmacyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by status") @RequestParam(required = false) OrderStatus status) {

        Long pharmacyId = getCurrentPharmacyId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.findByPharmacy(pharmacyId, pageable);
        Page<OrderResponse> responsePage = orders.map(orderMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    @GetMapping("/staff/orders/status/{status}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get orders by status",
            description = "List orders filtered by status",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        Long pharmacyId = getCurrentPharmacyId();

        List<Order> orders = orderService.findByPharmacyAndStatus(pharmacyId, status);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/staff/orders/pending")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get pending orders",
            description = "List all pending orders (quick access)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Order> orders = orderService.findPendingOrders(pharmacyId);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/staff/orders/recent")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get recent orders",
            description = "List 10 most recent orders",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<OrderResponse>> getRecentOrders() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Order> orders = orderService.findRecentOrders(pharmacyId);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/staff/orders/{orderNumber}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get order details (Staff)",
            description = "Get full order details including items",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        Long pharmacyId = getCurrentPharmacyId();

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        return ResponseEntity.ok(orderMapper.toResponseWithItems(order));
    }

    @PatchMapping("/staff/orders/{orderNumber}/status")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Update order status",
            description = "Update order status (follows allowed transitions)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        OrderStatus oldStatus = order.getStatus();

        order = orderService.updateStatus(order.getId(), request.getStatus());

        if (request.getTrackingNumber() != null && request.getStatus() == OrderStatus.SHIPPED) {
            order = orderService.setTrackingNumber(order.getId(), request.getTrackingNumber(), request.getCargoCompany());

            auditLogService.logOrderShipped(
                    pharmacyId, userId, userEmail,
                    order.getId(), order.getOrderNumber(),
                    request.getTrackingNumber(), request.getCargoCompany()
            );
        } else {
            auditLogService.logOrderStatusChanged(
                    pharmacyId, userId, userEmail,
                    order.getId(), order.getOrderNumber(),
                    oldStatus.name(), request.getStatus().name()
            );
        }

        log.info("Order status updated: {} from {} to {} by user: {}",
                order.getOrderNumber(), oldStatus, request.getStatus(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Order status updated", orderMapper.toResponse(order)));
    }

    @PostMapping("/staff/orders/{orderNumber}/cancel")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Cancel order (Staff)",
            description = "Cancel an order with reason",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrderByStaff(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderCancelRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        order = orderService.cancelOrder(order.getId(), request.getReason(), userId);

        auditLogService.logOrderCancelled(
                pharmacyId, userId, userEmail,
                order.getId(), order.getOrderNumber(), request.getReason()
        );

        log.info("Order cancelled by staff: {} - {}", order.getOrderNumber(), request.getReason());

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", orderMapper.toResponse(order)));
    }

    @PatchMapping("/staff/orders/{orderNumber}/tracking")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Update tracking number",
            description = "Add or update cargo tracking information",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> updateTrackingNumber(
            @PathVariable String orderNumber,
            @Parameter(description = "Tracking number") @RequestParam String trackingNumber,
            @Parameter(description = "Cargo company name") @RequestParam String cargoCompany) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        order = orderService.setTrackingNumber(order.getId(), trackingNumber, cargoCompany);

        auditLogService.logOrderTrackingUpdated(
                pharmacyId, userId, userEmail,
                order.getId(), order.getOrderNumber(),
                trackingNumber, cargoCompany
        );

        return ResponseEntity.ok(ApiResponse.success("Tracking number updated", orderMapper.toResponse(order)));
    }

    @GetMapping("/staff/orders/stats")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get order statistics",
            description = "Get order count by status",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        Long pharmacyId = getCurrentPharmacyId();

        long pending = orderService.countByStatus(pharmacyId, OrderStatus.PENDING);
        long confirmed = orderService.countByStatus(pharmacyId, OrderStatus.CONFIRMED);
        long preparing = orderService.countByStatus(pharmacyId, OrderStatus.PREPARING);
        long shipped = orderService.countByStatus(pharmacyId, OrderStatus.SHIPPED);
        long delivered = orderService.countByStatus(pharmacyId, OrderStatus.DELIVERED);
        long cancelled = orderService.countByStatus(pharmacyId, OrderStatus.CANCELLED);
        long todayOrders = orderService.countTodayOrders(pharmacyId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", pending);
        stats.put("confirmed", confirmed);
        stats.put("preparing", preparing);
        stats.put("shipped", shipped);
        stats.put("delivered", delivered);
        stats.put("cancelled", cancelled);
        stats.put("todayOrders", todayOrders);

        return ResponseEntity.ok(stats);
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentPharmacyId() {
        return securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));
    }

    private void validateOrderBelongsToPharmacy(Order order, Long pharmacyId) {
        if (!order.getPharmacy().getId().equals(pharmacyId)) {
            throw AccessDeniedException.resourceAccess("order");
        }
    }
}