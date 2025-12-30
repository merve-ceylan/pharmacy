package com.pharmacy.service;

import com.pharmacy.entity.AuditLog;
import com.pharmacy.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== CORE LOG METHOD ====================

    public AuditLog log(Long pharmacyId, Long userId, String userEmail,
                        String actionType, String entityType, Long entityId,
                        String oldValue, String newValue, String description,
                        String ipAddress, String userAgent, String requestUrl, String requestMethod) {

        AuditLog log = AuditLog.builder()
                .pharmacyId(pharmacyId)
                .userId(userId)
                .userEmail(userEmail)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .requestUrl(requestUrl)
                .requestMethod(requestMethod)
                .build();

        return auditLogRepository.save(log);
    }

    // Simple log without request info
    public AuditLog log(Long pharmacyId, Long userId, String userEmail,
                        String actionType, String entityType, Long entityId,
                        String oldValue, String newValue, String description) {
        return log(pharmacyId, userId, userEmail, actionType, entityType, entityId,
                oldValue, newValue, description, null, null, null, null);
    }

    // ==================== USER LOGS ====================

    public AuditLog logUserLogin(Long userId, String userEmail, String ipAddress, String userAgent) {
        return log(null, userId, userEmail, "USER_LOGIN", "USER", userId,
                null, null, "User logged in successfully",
                ipAddress, userAgent, "/api/auth/login", "POST");
    }

    public AuditLog logUserLogout(Long userId, String userEmail, String ipAddress) {
        return log(null, userId, userEmail, "USER_LOGOUT", "USER", userId,
                null, null, "User logged out",
                ipAddress, null, "/api/auth/logout", "POST");
    }

    public AuditLog logUserLoginFailed(String userEmail, String ipAddress, String reason) {
        return log(null, null, userEmail, "USER_LOGIN_FAILED", "USER", null,
                null, null, "Login failed: " + reason,
                ipAddress, null, "/api/auth/login", "POST");
    }

    public AuditLog logUserAccountLocked(Long userId, String userEmail, int failedAttempts) {
        return log(null, userId, userEmail, "USER_ACCOUNT_LOCKED", "USER", userId,
                null, String.valueOf(failedAttempts),
                "Account locked after " + failedAttempts + " failed attempts",
                null, null, null, null);
    }

    public AuditLog logUserCreated(Long pharmacyId, Long createdBy, String createdByEmail,
                                   Long newUserId, String newUserEmail, String role) {
        return log(pharmacyId, createdBy, createdByEmail, "USER_CREATED", "USER", newUserId,
                null, newUserEmail, "New user created with role: " + role,
                null, null, null, null);
    }

    public AuditLog logUserUpdated(Long pharmacyId, Long updatedBy, String updatedByEmail,
                                   Long userId, String changes) {
        return log(pharmacyId, updatedBy, updatedByEmail, "USER_UPDATED", "USER", userId,
                null, changes, "User profile updated",
                null, null, null, null);
    }

    public AuditLog logUserDeactivated(Long pharmacyId, Long deactivatedBy, String deactivatedByEmail,
                                       Long userId, String userEmail) {
        return log(pharmacyId, deactivatedBy, deactivatedByEmail, "USER_DEACTIVATED", "USER", userId,
                "active", "inactive", "User deactivated: " + userEmail,
                null, null, null, null);
    }

    public AuditLog logUserActivated(Long pharmacyId, Long activatedBy, String activatedByEmail,
                                     Long userId, String userEmail) {
        return log(pharmacyId, activatedBy, activatedByEmail, "USER_ACTIVATED", "USER", userId,
                "inactive", "active", "User activated: " + userEmail,
                null, null, null, null);
    }

    public AuditLog logPasswordChanged(Long userId, String userEmail, String ipAddress) {
        return log(null, userId, userEmail, "PASSWORD_CHANGED", "USER", userId,
                null, null, "Password changed by user",
                ipAddress, null, null, null);
    }

    public AuditLog logPasswordReset(Long userId, String userEmail, Long resetBy, String resetByEmail) {
        return log(null, resetBy, resetByEmail, "PASSWORD_RESET", "USER", userId,
                null, null, "Password reset for user: " + userEmail,
                null, null, null, null);
    }

    // ==================== PHARMACY LOGS ====================

    public AuditLog logPharmacyCreated(Long adminId, String adminEmail, Long pharmacyId,
                                       String pharmacyName, String subscriptionPlan) {
        return log(pharmacyId, adminId, adminEmail, "PHARMACY_CREATED", "PHARMACY", pharmacyId,
                null, pharmacyName, "Pharmacy created with plan: " + subscriptionPlan,
                null, null, null, null);
    }

    public AuditLog logPharmacyUpdated(Long pharmacyId, Long updatedBy, String updatedByEmail,
                                       String oldValues, String newValues) {
        return log(pharmacyId, updatedBy, updatedByEmail, "PHARMACY_UPDATED", "PHARMACY", pharmacyId,
                oldValues, newValues, "Pharmacy settings updated",
                null, null, null, null);
    }

    public AuditLog logPharmacySuspended(Long adminId, String adminEmail, Long pharmacyId,
                                         String pharmacyName, String reason) {
        return log(pharmacyId, adminId, adminEmail, "PHARMACY_SUSPENDED", "PHARMACY", pharmacyId,
                "ACTIVE", "SUSPENDED", "Pharmacy suspended: " + reason,
                null, null, null, null);
    }

    public AuditLog logPharmacyActivated(Long adminId, String adminEmail, Long pharmacyId,
                                         String pharmacyName) {
        return log(pharmacyId, adminId, adminEmail, "PHARMACY_ACTIVATED", "PHARMACY", pharmacyId,
                "SUSPENDED", "ACTIVE", "Pharmacy reactivated: " + pharmacyName,
                null, null, null, null);
    }

    public AuditLog logPharmacyPlanUpgraded(Long pharmacyId, Long userId, String userEmail,
                                            String oldPlan, String newPlan) {
        return log(pharmacyId, userId, userEmail, "PHARMACY_PLAN_UPGRADED", "PHARMACY", pharmacyId,
                oldPlan, newPlan, "Subscription plan upgraded from " + oldPlan + " to " + newPlan,
                null, null, null, null);
    }

    public AuditLog logPharmacyGracePeriodStarted(Long pharmacyId, String pharmacyName) {
        return log(pharmacyId, null, null, "PHARMACY_GRACE_PERIOD", "PHARMACY", pharmacyId,
                "ACTIVE", "GRACE_PERIOD", "Grace period started for: " + pharmacyName,
                null, null, null, null);
    }

    // ==================== PRODUCT LOGS ====================

    public AuditLog logProductCreated(Long pharmacyId, Long userId, String userEmail,
                                      Long productId, String productName, String sku) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_CREATED", "PRODUCT", productId,
                null, productName, "Product created: " + productName + " (SKU: " + sku + ")",
                null, null, null, null);
    }

    public AuditLog logProductUpdated(Long pharmacyId, Long userId, String userEmail,
                                      Long productId, String productName, String changes) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_UPDATED", "PRODUCT", productId,
                null, changes, "Product updated: " + productName,
                null, null, null, null);
    }

    public AuditLog logProductDeleted(Long pharmacyId, Long userId, String userEmail,
                                      Long productId, String productName) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_DELETED", "PRODUCT", productId,
                productName, null, "Product deleted: " + productName,
                null, null, null, null);
    }

    public AuditLog logProductActivated(Long pharmacyId, Long userId, String userEmail,
                                        Long productId, String productName) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_ACTIVATED", "PRODUCT", productId,
                "inactive", "active", "Product activated: " + productName,
                null, null, null, null);
    }

    public AuditLog logProductDeactivated(Long pharmacyId, Long userId, String userEmail,
                                          Long productId, String productName) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_DEACTIVATED", "PRODUCT", productId,
                "active", "inactive", "Product deactivated: " + productName,
                null, null, null, null);
    }

    public AuditLog logProductPriceChanged(Long pharmacyId, Long userId, String userEmail,
                                           Long productId, String productName,
                                           BigDecimal oldPrice, BigDecimal newPrice) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_PRICE_CHANGED", "PRODUCT", productId,
                oldPrice.toString(), newPrice.toString(),
                "Price changed for " + productName + ": " + oldPrice + " -> " + newPrice,
                null, null, null, null);
    }

    public AuditLog logProductStockChanged(Long pharmacyId, Long userId, String userEmail,
                                           Long productId, String productName,
                                           Integer oldStock, Integer newStock, String reason) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_STOCK_CHANGED", "PRODUCT", productId,
                String.valueOf(oldStock), String.valueOf(newStock),
                "Stock changed for " + productName + ": " + oldStock + " -> " + newStock + " (" + reason + ")",
                null, null, null, null);
    }

    public AuditLog logProductLowStock(Long pharmacyId, Long productId, String productName,
                                       Integer currentStock, Integer threshold) {
        return log(pharmacyId, null, null, "PRODUCT_LOW_STOCK", "PRODUCT", productId,
                null, String.valueOf(currentStock),
                "Low stock alert: " + productName + " has " + currentStock + " items (threshold: " + threshold + ")",
                null, null, null, null);
    }

    public AuditLog logProductOutOfStock(Long pharmacyId, Long productId, String productName) {
        return log(pharmacyId, null, null, "PRODUCT_OUT_OF_STOCK", "PRODUCT", productId,
                null, "0", "Out of stock: " + productName,
                null, null, null, null);
    }

    public AuditLog logProductBulkImport(Long pharmacyId, Long userId, String userEmail,
                                         int successCount, int failCount) {
        return log(pharmacyId, userId, userEmail, "PRODUCT_BULK_IMPORT", "PRODUCT", null,
                null, successCount + " success, " + failCount + " failed",
                "Bulk product import completed",
                null, null, null, null);
    }

    // ==================== CATEGORY LOGS ====================

    public AuditLog logCategoryCreated(Long userId, String userEmail, Long categoryId, String categoryName) {
        return log(null, userId, userEmail, "CATEGORY_CREATED", "CATEGORY", categoryId,
                null, categoryName, "Category created: " + categoryName,
                null, null, null, null);
    }

    public AuditLog logCategoryUpdated(Long userId, String userEmail, Long categoryId,
                                       String categoryName, String changes) {
        return log(null, userId, userEmail, "CATEGORY_UPDATED", "CATEGORY", categoryId,
                null, changes, "Category updated: " + categoryName,
                null, null, null, null);
    }

    public AuditLog logCategoryDeleted(Long userId, String userEmail, Long categoryId, String categoryName) {
        return log(null, userId, userEmail, "CATEGORY_DELETED", "CATEGORY", categoryId,
                categoryName, null, "Category deleted: " + categoryName,
                null, null, null, null);
    }

    // ==================== ORDER LOGS ====================

    public AuditLog logOrderCreated(Long pharmacyId, Long customerId, String customerEmail,
                                    Long orderId, String orderNumber, BigDecimal totalAmount) {
        return log(pharmacyId, customerId, customerEmail, "ORDER_CREATED", "ORDER", orderId,
                null, orderNumber, "Order created: " + orderNumber + " (Total: " + totalAmount + " TL)",
                null, null, null, null);
    }

    public AuditLog logOrderStatusChanged(Long pharmacyId, Long userId, String userEmail,
                                          Long orderId, String orderNumber,
                                          String oldStatus, String newStatus) {
        return log(pharmacyId, userId, userEmail, "ORDER_STATUS_CHANGED", "ORDER", orderId,
                oldStatus, newStatus,
                "Order " + orderNumber + " status changed: " + oldStatus + " -> " + newStatus,
                null, null, null, null);
    }

    public AuditLog logOrderConfirmed(Long pharmacyId, Long userId, String userEmail,
                                      Long orderId, String orderNumber) {
        return log(pharmacyId, userId, userEmail, "ORDER_CONFIRMED", "ORDER", orderId,
                "PENDING", "CONFIRMED", "Order confirmed: " + orderNumber,
                null, null, null, null);
    }

    public AuditLog logOrderPreparing(Long pharmacyId, Long userId, String userEmail,
                                      Long orderId, String orderNumber) {
        return log(pharmacyId, userId, userEmail, "ORDER_PREPARING", "ORDER", orderId,
                "CONFIRMED", "PREPARING", "Order preparation started: " + orderNumber,
                null, null, null, null);
    }

    public AuditLog logOrderShipped(Long pharmacyId, Long userId, String userEmail,
                                    Long orderId, String orderNumber, String trackingNumber,
                                    String cargoCompany) {
        String details = trackingNumber != null ?
                " (Tracking: " + trackingNumber + ", Cargo: " + cargoCompany + ")" : "";
        return log(pharmacyId, userId, userEmail, "ORDER_SHIPPED", "ORDER", orderId,
                "PREPARING", "SHIPPED", "Order shipped: " + orderNumber + details,
                null, null, null, null);
    }

    public AuditLog logOrderDelivered(Long pharmacyId, Long userId, String userEmail,
                                      Long orderId, String orderNumber) {
        return log(pharmacyId, userId, userEmail, "ORDER_DELIVERED", "ORDER", orderId,
                "SHIPPED", "DELIVERED", "Order delivered: " + orderNumber,
                null, null, null, null);
    }

    public AuditLog logOrderCancelled(Long pharmacyId, Long userId, String userEmail,
                                      Long orderId, String orderNumber, String reason) {
        return log(pharmacyId, userId, userEmail, "ORDER_CANCELLED", "ORDER", orderId,
                null, "CANCELLED", "Order cancelled: " + orderNumber + " (Reason: " + reason + ")",
                null, null, null, null);
    }

    public AuditLog logOrderTrackingUpdated(Long pharmacyId, Long userId, String userEmail,
                                            Long orderId, String orderNumber,
                                            String trackingNumber, String cargoCompany) {
        return log(pharmacyId, userId, userEmail, "ORDER_TRACKING_UPDATED", "ORDER", orderId,
                null, trackingNumber,
                "Tracking updated for " + orderNumber + ": " + trackingNumber + " (" + cargoCompany + ")",
                null, null, null, null);
    }

    // ==================== PAYMENT LOGS ====================

    public AuditLog logPaymentInitiated(Long pharmacyId, Long customerId, String customerEmail,
                                        Long paymentId, Long orderId, String orderNumber,
                                        BigDecimal amount, String conversationId) {
        return log(pharmacyId, customerId, customerEmail, "PAYMENT_INITIATED", "PAYMENT", paymentId,
                null, amount.toString(),
                "Payment initiated for order " + orderNumber + ": " + amount + " TL (ConvID: " + conversationId + ")",
                null, null, null, null);
    }

    public AuditLog logPaymentSuccess(Long pharmacyId, Long customerId, String customerEmail,
                                      Long paymentId, Long orderId, String orderNumber,
                                      BigDecimal amount, String transactionId, String cardLastFour) {
        return log(pharmacyId, customerId, customerEmail, "PAYMENT_SUCCESS", "PAYMENT", paymentId,
                "PENDING", "SUCCESS",
                "Payment successful for order " + orderNumber + ": " + amount + " TL (TxnID: " + transactionId + ", Card: *" + cardLastFour + ")",
                null, null, null, null);
    }

    public AuditLog logPaymentFailed(Long pharmacyId, Long customerId, String customerEmail,
                                     Long paymentId, Long orderId, String orderNumber,
                                     String errorCode, String errorMessage) {
        return log(pharmacyId, customerId, customerEmail, "PAYMENT_FAILED", "PAYMENT", paymentId,
                "PENDING", "FAILED",
                "Payment failed for order " + orderNumber + ": " + errorCode + " - " + errorMessage,
                null, null, null, null);
    }

    public AuditLog logPaymentRefundInitiated(Long pharmacyId, Long userId, String userEmail,
                                              Long paymentId, Long orderId, String orderNumber,
                                              BigDecimal refundAmount) {
        return log(pharmacyId, userId, userEmail, "PAYMENT_REFUND_INITIATED", "PAYMENT", paymentId,
                null, refundAmount.toString(),
                "Refund initiated for order " + orderNumber + ": " + refundAmount + " TL",
                null, null, null, null);
    }

    public AuditLog logPaymentRefundSuccess(Long pharmacyId, Long userId, String userEmail,
                                            Long paymentId, Long orderId, String orderNumber,
                                            BigDecimal refundAmount) {
        return log(pharmacyId, userId, userEmail, "PAYMENT_REFUND_SUCCESS", "PAYMENT", paymentId,
                null, refundAmount.toString(),
                "Refund successful for order " + orderNumber + ": " + refundAmount + " TL",
                null, null, null, null);
    }

    public AuditLog logPaymentRefundFailed(Long pharmacyId, Long userId, String userEmail,
                                           Long paymentId, Long orderId, String orderNumber,
                                           String errorMessage) {
        return log(pharmacyId, userId, userEmail, "PAYMENT_REFUND_FAILED", "PAYMENT", paymentId,
                null, null,
                "Refund failed for order " + orderNumber + ": " + errorMessage,
                null, null, null, null);
    }

    // ==================== CART LOGS ====================

    public AuditLog logCartItemAdded(Long pharmacyId, Long customerId, String customerEmail,
                                     Long cartId, Long productId, String productName,
                                     Integer quantity) {
        return log(pharmacyId, customerId, customerEmail, "CART_ITEM_ADDED", "CART", cartId,
                null, productName + " x " + quantity,
                "Added to cart: " + productName + " (Qty: " + quantity + ")",
                null, null, null, null);
    }

    public AuditLog logCartItemUpdated(Long pharmacyId, Long customerId, String customerEmail,
                                       Long cartId, Long productId, String productName,
                                       Integer oldQuantity, Integer newQuantity) {
        return log(pharmacyId, customerId, customerEmail, "CART_ITEM_UPDATED", "CART", cartId,
                String.valueOf(oldQuantity), String.valueOf(newQuantity),
                "Cart updated: " + productName + " quantity " + oldQuantity + " -> " + newQuantity,
                null, null, null, null);
    }

    public AuditLog logCartItemRemoved(Long pharmacyId, Long customerId, String customerEmail,
                                       Long cartId, Long productId, String productName) {
        return log(pharmacyId, customerId, customerEmail, "CART_ITEM_REMOVED", "CART", cartId,
                productName, null, "Removed from cart: " + productName,
                null, null, null, null);
    }

    public AuditLog logCartCleared(Long pharmacyId, Long customerId, String customerEmail,
                                   Long cartId, String reason) {
        return log(pharmacyId, customerId, customerEmail, "CART_CLEARED", "CART", cartId,
                null, null, "Cart cleared: " + reason,
                null, null, null, null);
    }

    // ==================== SUBSCRIPTION LOGS ====================

    public AuditLog logSubscriptionPaymentSuccess(Long pharmacyId, String pharmacyName,
                                                  BigDecimal amount, String period) {
        return log(pharmacyId, null, null, "SUBSCRIPTION_PAYMENT_SUCCESS", "PHARMACY", pharmacyId,
                null, amount.toString(),
                "Subscription payment received for " + pharmacyName + ": " + amount + " TL (" + period + ")",
                null, null, null, null);
    }

    public AuditLog logSubscriptionPaymentFailed(Long pharmacyId, String pharmacyName, String reason) {
        return log(pharmacyId, null, null, "SUBSCRIPTION_PAYMENT_FAILED", "PHARMACY", pharmacyId,
                null, null,
                "Subscription payment failed for " + pharmacyName + ": " + reason,
                null, null, null, null);
    }

    public AuditLog logSubscriptionRenewal(Long pharmacyId, String pharmacyName,
                                           BigDecimal amount, String newEndDate) {
        return log(pharmacyId, null, null, "SUBSCRIPTION_RENEWED", "PHARMACY", pharmacyId,
                null, newEndDate,
                "Subscription renewed for " + pharmacyName + ": " + amount + " TL (until " + newEndDate + ")",
                null, null, null, null);
    }

    // ==================== SYSTEM LOGS ====================

    public AuditLog logSystemError(String errorType, String errorMessage, String stackTrace) {
        return log(null, null, null, "SYSTEM_ERROR", "SYSTEM", null,
                null, errorMessage, "System error: " + errorType,
                null, null, null, null);
    }

    public AuditLog logApiRequest(Long pharmacyId, Long userId, String userEmail,
                                  String endpoint, String method, String ipAddress,
                                  int responseStatus, long responseTime) {
        return log(pharmacyId, userId, userEmail, "API_REQUEST", "SYSTEM", null,
                null, String.valueOf(responseStatus),
                method + " " + endpoint + " - " + responseStatus + " (" + responseTime + "ms)",
                ipAddress, null, endpoint, method);
    }

    // ==================== QUERY METHODS ====================

    public Page<AuditLog> findByPharmacy(Long pharmacyId, Pageable pageable) {
        return auditLogRepository.findByPharmacyId(pharmacyId, pageable);
    }

    public List<AuditLog> findByUser(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    public List<AuditLog> findByActionType(String actionType) {
        return auditLogRepository.findByActionType(actionType);
    }

    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    public Page<AuditLog> findByPharmacyAndActionType(Long pharmacyId, String actionType, Pageable pageable) {
        return auditLogRepository.findByPharmacyIdAndActionType(pharmacyId, actionType, pageable);
    }

    public Page<AuditLog> findByPharmacyAndEntityType(Long pharmacyId, String entityType, Pageable pageable) {
        return auditLogRepository.findByPharmacyIdAndEntityType(pharmacyId, entityType, pageable);
    }

    public Page<AuditLog> findByPharmacyAndDateRange(Long pharmacyId, LocalDateTime startDate,
                                                     LocalDateTime endDate, Pageable pageable) {
        return auditLogRepository.findByPharmacyIdAndCreatedAtBetween(pharmacyId, startDate, endDate, pageable);
    }

    public List<AuditLog> findRecentLogs(Long pharmacyId) {
        return auditLogRepository.findTop50ByPharmacyIdOrderByCreatedAtDesc(pharmacyId);
    }

    public Page<AuditLog> findSystemLogs(Pageable pageable) {
        return auditLogRepository.findByPharmacyIdIsNull(pageable);
    }

    // Helper method to convert object to JSON
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }
}
