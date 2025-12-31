package com.pharmacy.controller;

import com.pharmacy.dto.request.PaymentInitRequest;
import com.pharmacy.dto.request.RefundRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.PaymentResponse;
import com.pharmacy.entity.Order;
import com.pharmacy.entity.Payment;
import com.pharmacy.enums.PaymentStatus;
import com.pharmacy.exception.AccessDeniedException;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.exception.BusinessException;
import com.pharmacy.mapper.PaymentMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.OrderService;
import com.pharmacy.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Payments", description = "Payment processing endpoints")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentMapper paymentMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public PaymentController(PaymentService paymentService,
                             OrderService orderService,
                             PaymentMapper paymentMapper,
                             SecurityUtils securityUtils,
                             AuditLogService auditLogService) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.paymentMapper = paymentMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    // ==================== CUSTOMER ENDPOINTS ====================

    @PostMapping("/customer/payments/init")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Initialize payment",
            description = "Initialize a payment for an order (returns payment form or 3D Secure redirect)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment initialized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order not in pending status")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> initializePayment(
            @Valid @RequestBody PaymentInitRequest request) {

        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String customerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Order order = orderService.getById(request.getOrderId());

        if (!order.getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        if (order.getStatus() != com.pharmacy.enums.OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in pending status");
        }

        Payment payment = paymentService.createPayment(order);

        auditLogService.logPaymentInitiated(
                order.getPharmacy().getId(), customerId, customerEmail,
                payment.getId(), order.getId(), order.getOrderNumber(),
                payment.getAmount(), payment.getConversationId()
        );

        log.info("Payment initialized for order: {} by customer: {}", order.getOrderNumber(), customerEmail);

        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setRedirectUrl("/payment/checkout?conversationId=" + payment.getConversationId());

        return ResponseEntity.ok(ApiResponse.success("Payment initialized", response));
    }

    @GetMapping("/customer/payments/{paymentId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get payment status",
            description = "Get current status of a payment",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId) {
        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        Payment payment = paymentService.getById(paymentId);

        if (!payment.getOrder().getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("payment");
        }

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @GetMapping("/customer/payments/order/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
            summary = "Get payment by order",
            description = "Get payment details for an order",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable String orderNumber) {
        Long customerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        Order order = orderService.getByOrderNumber(orderNumber);

        if (!order.getCustomer().getId().equals(customerId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        Payment payment = paymentService.findByOrder(order.getId())
                .orElseThrow(() -> new BadRequestException("No payment found for this order"));

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    // ==================== IYZICO CALLBACK ENDPOINTS ====================

    @PostMapping("/public/payments/callback")
    @Operation(
            summary = "Payment callback",
            description = "Handle 3D Secure callback from payment provider (iyzico)"
    )
    public ResponseEntity<Map<String, Object>> handlePaymentCallback(
            HttpServletRequest request,
            @RequestParam Map<String, String> params) {

        log.info("Payment callback received with params: {}", params.keySet());

        String status = params.get("status");
        String conversationId = params.get("conversationId");
        String paymentId = params.get("paymentId");

        if (conversationId == null || conversationId.isEmpty()) {
            log.error("Payment callback missing conversationId");
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Missing conversationId"
            ));
        }

        try {
            Payment payment = paymentService.getByConversationId(conversationId);
            Long pharmacyId = payment.getOrder().getPharmacy().getId();

            if ("success".equalsIgnoreCase(status)) {
                String transactionId = params.get("transactionId");
                String cardLastFour = params.get("cardLastFour");
                String cardBrand = params.get("cardBrand");

                payment = paymentService.processSuccessfulPayment(
                        conversationId, transactionId, paymentId, cardLastFour, cardBrand);

                auditLogService.logPaymentSuccess(
                        pharmacyId, payment.getOrder().getCustomer().getId(),
                        payment.getOrder().getCustomer().getEmail(),
                        payment.getId(), payment.getOrder().getId(),
                        payment.getOrder().getOrderNumber(),
                        payment.getAmount(), transactionId, cardLastFour
                );

                log.info("Payment successful for order: {}", payment.getOrder().getOrderNumber());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment successful",
                        "orderNumber", payment.getOrder().getOrderNumber(),
                        "redirectUrl", "/orders/" + payment.getOrder().getOrderNumber() + "/success"
                ));
            } else {
                String errorCode = params.get("errorCode");
                String errorMessage = params.get("errorMessage");

                payment = paymentService.processFailedPayment(conversationId, errorCode, errorMessage);

                auditLogService.logPaymentFailed(
                        pharmacyId, payment.getOrder().getCustomer().getId(),
                        payment.getOrder().getCustomer().getEmail(),
                        payment.getId(), payment.getOrder().getId(),
                        payment.getOrder().getOrderNumber(),
                        errorCode, errorMessage
                );

                log.warn("Payment failed for order: {} - {}: {}",
                        payment.getOrder().getOrderNumber(), errorCode, errorMessage);

                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", errorMessage != null ? errorMessage : "Payment failed",
                        "errorCode", errorCode != null ? errorCode : "UNKNOWN",
                        "redirectUrl", "/orders/" + payment.getOrder().getOrderNumber() + "/failed"
                ));
            }
        } catch (Exception e) {
            log.error("Error processing payment callback", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Error processing payment"
            ));
        }
    }

    @PostMapping("/public/payments/webhook")
    @Operation(
            summary = "Payment webhook",
            description = "Handle async payment notifications from payment provider"
    )
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Iyzico-Signature", required = false) String signature) {

        log.info("Payment webhook received");

        // TODO: Validate signature
        // TODO: Process webhook payload

        return ResponseEntity.ok("OK");
    }

    // ==================== STAFF ENDPOINTS ====================

    @GetMapping("/staff/payments/order/{orderNumber}")
    @PreAuthorize("hasAnyRole('PHARMACY_OWNER', 'STAFF')")
    @Operation(
            summary = "Get payment for order (Staff)",
            description = "Get payment details for an order",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PaymentResponse> getPaymentForOrder(@PathVariable String orderNumber) {
        Long pharmacyId = getCurrentPharmacyId();

        Order order = orderService.getByOrderNumber(orderNumber);

        if (!order.getPharmacy().getId().equals(pharmacyId)) {
            throw AccessDeniedException.resourceAccess("order");
        }

        Payment payment = paymentService.findByOrder(order.getId())
                .orElseThrow(() -> new BadRequestException("No payment found for this order"));

        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @PostMapping("/staff/payments/refund")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Process refund",
            description = "Process a full or partial refund (Pharmacy Owner only)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund processed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Payment cannot be refunded")
    })
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @Valid @RequestBody RefundRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Payment payment = paymentService.getById(request.getPaymentId());

        if (!payment.getOrder().getPharmacy().getId().equals(pharmacyId)) {
            throw AccessDeniedException.resourceAccess("payment");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw BusinessException.refundNotAllowed();
        }

        auditLogService.logPaymentRefundInitiated(
                pharmacyId, userId, userEmail,
                payment.getId(), payment.getOrder().getId(),
                payment.getOrder().getOrderNumber(),
                request.getAmount() != null ? request.getAmount() : payment.getAmount()
        );

        Payment refundedPayment;
        if (request.isFullRefund()) {
            refundedPayment = paymentService.processFullRefund(payment.getId());
        } else {
            refundedPayment = paymentService.processPartialRefund(payment.getId(), request.getAmount());
        }

        auditLogService.logPaymentRefundSuccess(
                pharmacyId, userId, userEmail,
                refundedPayment.getId(), refundedPayment.getOrder().getId(),
                refundedPayment.getOrder().getOrderNumber(),
                refundedPayment.getRefundedAmount()
        );

        log.info("Refund processed for payment: {} by user: {}", payment.getId(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully",
                paymentMapper.toResponse(refundedPayment)));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/payments/{paymentId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get payment (Admin)",
            description = "Get payment details by ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PaymentResponse> getPaymentAdmin(@PathVariable Long paymentId) {
        Payment payment = paymentService.getById(paymentId);
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @GetMapping("/admin/payments/transaction/{transactionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get payment by transaction ID",
            description = "Find payment by payment provider transaction ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PaymentResponse> getPaymentByTransaction(@PathVariable String transactionId) {
        Payment payment = paymentService.findByTransactionId(transactionId)
                .orElseThrow(() -> new BadRequestException("Payment not found"));
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentPharmacyId() {
        return securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));
    }
}