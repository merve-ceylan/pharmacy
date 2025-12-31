package com.pharmacy.controller;

import com.pharmacy.dto.request.OrderCancelRequest;
import com.pharmacy.dto.request.OrderCreateRequest;
import com.pharmacy.dto.request.OrderStatusUpdateRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.OrderResponse;
import com.pharmacy.dto.response.PageResponse;
import com.pharmacy.entity.Cart;
import com.pharmacy.entity.Order;
import com.pharmacy.entity.User;
import com.pharmacy.enums.OrderStatus;
import com.pharmacy.exception.AccessDeniedException;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.mapper.OrderMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.CartService;
import com.pharmacy.service.OrderService;
import com.pharmacy.service.PharmacyService;
import com.pharmacy.service.UserService;
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
import java.util.Map;

@RestController
@RequestMapping("/api")
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

    /**
     * Create order from cart
     * POST /api/customer/orders
     */
    @PostMapping("/customer/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderCreateRequest request) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        // Validate pharmacy is active
        pharmacyService.validatePharmacyActive(request.getPharmacyId());

        // Get customer's cart for this pharmacy
        Cart cart = cartService.getCart(customerId, request.getPharmacyId())
                .orElseThrow(() -> new BadRequestException("Cart is empty"));

        // Create order from cart
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

        // Audit log
        auditLogService.logOrderCreated(
                request.getPharmacyId(), customerId, customerEmail,
                order.getId(), order.getOrderNumber(), order.getTotalAmount()
        );

        log.info("Order created: {} by customer: {}", order.getOrderNumber(), customerEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", orderMapper.toResponseWithItems(order)));
    }

    /**
     * Get customer's orders
     * GET /api/customer/orders
     */
    @GetMapping("/customer/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<OrderResponse>> getCustomerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.findByCustomer(customerId, pageable);
        Page<OrderResponse> responsePage = orders.map(orderMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    /**
     * Get single order details (customer)
     * GET /api/customer/orders/{orderNumber}
     */
    @GetMapping("/customer/orders/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getCustomerOrder(@PathVariable String orderNumber) {
        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        Order order = orderService.getByOrderNumber(orderNumber);

        // Validate order belongs to customer
        if (!order.getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        return ResponseEntity.ok(orderMapper.toResponseWithItems(order));
    }

    /**
     * Cancel order (customer)
     * POST /api/customer/orders/{orderNumber}/cancel
     */
    @PostMapping("/customer/orders/{orderNumber}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrderByCustomer(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderCancelRequest request) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);

        // Validate order belongs to customer
        if (!order.getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        order = orderService.cancelOrder(order.getId(), request.getReason(), customerId);

        // Audit log
        auditLogService.logOrderCancelled(
                order.getPharmacy().getId(), customerId, customerEmail,
                order.getId(), order.getOrderNumber(), request.getReason()
        );

        log.info("Order cancelled by customer: {} - {}", order.getOrderNumber(), request.getReason());

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", orderMapper.toResponse(order)));
    }

    // ==================== STAFF ENDPOINTS (Pharmacy Owner & Staff) ====================

    /**
     * Get pharmacy orders
     * GET /api/staff/orders
     */
    @GetMapping("/staff/orders")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<PageResponse<OrderResponse>> getPharmacyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {

        Long pharmacyId = getCurrentPharmacyId();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orders = orderService.findByPharmacy(pharmacyId, pageable);
        Page<OrderResponse> responsePage = orders.map(orderMapper::toResponse);

        return ResponseEntity.ok(PageResponse.of(responsePage));
    }

    /**
     * Get orders by status
     * GET /api/staff/orders/status/{status}
     */
    @GetMapping("/staff/orders/status/{status}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable OrderStatus status) {
        Long pharmacyId = getCurrentPharmacyId();

        List<Order> orders = orderService.findByPharmacyAndStatus(pharmacyId, status);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get pending orders (for quick access)
     * GET /api/staff/orders/pending
     */
    @GetMapping("/staff/orders/pending")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<List<OrderResponse>> getPendingOrders() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Order> orders = orderService.findPendingOrders(pharmacyId);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get recent orders
     * GET /api/staff/orders/recent
     */
    @GetMapping("/staff/orders/recent")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<List<OrderResponse>> getRecentOrders() {
        Long pharmacyId = getCurrentPharmacyId();

        List<Order> orders = orderService.findRecentOrders(pharmacyId);
        List<OrderResponse> responses = orders.stream()
                .map(orderMapper::toResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get single order details (staff)
     * GET /api/staff/orders/{orderNumber}
     */
    @GetMapping("/staff/orders/{orderNumber}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderNumber) {
        Long pharmacyId = getCurrentPharmacyId();

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        return ResponseEntity.ok(orderMapper.toResponseWithItems(order));
    }

    /**
     * Update order status
     * PATCH /api/staff/orders/{orderNumber}/status
     */
    @PatchMapping("/staff/orders/{orderNumber}/status")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        OrderStatus oldStatus = order.getStatus();

        // Update status
        order = orderService.updateStatus(order.getId(), request.getStatus());

        // Set tracking info if provided
        if (request.getTrackingNumber() != null && request.getStatus() == OrderStatus.SHIPPED) {
            order = orderService.setTrackingNumber(order.getId(), request.getTrackingNumber(), request.getCargoCompany());

            auditLogService.logOrderShipped(
                    pharmacyId, userId, userEmail,
                    order.getId(), order.getOrderNumber(),
                    request.getTrackingNumber(), request.getCargoCompany()
            );
        } else {
            // Log status change
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

    /**
     * Cancel order (staff)
     * POST /api/staff/orders/{orderNumber}/cancel
     */
    @PostMapping("/staff/orders/{orderNumber}/cancel")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrderByStaff(
            @PathVariable String orderNumber,
            @Valid @RequestBody OrderCancelRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        order = orderService.cancelOrder(order.getId(), request.getReason(), userId);

        // Audit log
        auditLogService.logOrderCancelled(
                pharmacyId, userId, userEmail,
                order.getId(), order.getOrderNumber(), request.getReason()
        );

        log.info("Order cancelled by staff: {} - {}", order.getOrderNumber(), request.getReason());

        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", orderMapper.toResponse(order)));
    }

    /**
     * Update tracking number
     * PATCH /api/staff/orders/{orderNumber}/tracking
     */
    @PatchMapping("/staff/orders/{orderNumber}/tracking")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateTrackingNumber(
            @PathVariable String orderNumber,
            @RequestParam String trackingNumber,
            @RequestParam String cargoCompany) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getByOrderNumber(orderNumber);
        validateOrderBelongsToPharmacy(order, pharmacyId);

        order = orderService.setTrackingNumber(order.getId(), trackingNumber, cargoCompany);

        // Audit log
        auditLogService.logOrderTrackingUpdated(
                pharmacyId, userId, userEmail,
                order.getId(), order.getOrderNumber(),
                trackingNumber, cargoCompany
        );

        return ResponseEntity.ok(ApiResponse.success("Tracking number updated", orderMapper.toResponse(order)));
    }

    /**
     * Get order statistics
     * GET /api/staff/orders/stats
     */
    @GetMapping("/staff/orders/stats")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        Long pharmacyId = getCurrentPharmacyId();

        long pending = orderService.countByStatus(pharmacyId, OrderStatus.PENDING);
        long confirmed = orderService.countByStatus(pharmacyId, OrderStatus.CONFIRMED);
        long preparing = orderService.countByStatus(pharmacyId, OrderStatus.PREPARING);
        long shipped = orderService.countByStatus(pharmacyId, OrderStatus.SHIPPED);
        long todayOrders = orderService.countTodayOrders(pharmacyId);

        return ResponseEntity.ok(Map.of(
                "pending", pending,
                "confirmed", confirmed,
                "preparing", preparing,
                "shipped", shipped,
                "todayOrders", todayOrders
        ));
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