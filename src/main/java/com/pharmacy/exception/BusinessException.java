package com.pharmacy.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_ERROR");
    }

    public BusinessException(String message, String errorCode) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, errorCode);
    }

    // Stock related
    public static BusinessException insufficientStock(String productName, int available) {
        return new BusinessException("Insufficient stock for " + productName + ". Available: " + available, "INSUFFICIENT_STOCK");
    }

    public static BusinessException outOfStock(String productName) {
        return new BusinessException(productName + " is out of stock", "OUT_OF_STOCK");
    }

    // Order related
    public static BusinessException orderNotCancellable() {
        return new BusinessException("Order cannot be cancelled in current status", "ORDER_NOT_CANCELLABLE");
    }

    public static BusinessException invalidStatusTransition(String from, String to) {
        return new BusinessException("Invalid status transition: " + from + " -> " + to, "INVALID_STATUS_TRANSITION");
    }

    // Payment related
    public static BusinessException paymentFailed(String reason) {
        return new BusinessException("Payment failed: " + reason, "PAYMENT_FAILED");
    }

    public static BusinessException refundNotAllowed() {
        return new BusinessException("Refund is not allowed for this payment", "REFUND_NOT_ALLOWED");
    }

    // Cart related
    public static BusinessException emptyCart() {
        return new BusinessException("Cart is empty", "EMPTY_CART");
    }

    public static BusinessException cartItemsUnavailable() {
        return new BusinessException("Some items in cart are no longer available", "CART_ITEMS_UNAVAILABLE");
    }

    // Subscription related
    public static BusinessException subscriptionExpired() {
        return new BusinessException("Pharmacy subscription has expired", "SUBSCRIPTION_EXPIRED");
    }

    public static BusinessException pharmacySuspended() {
        return new BusinessException("Pharmacy is suspended", "PHARMACY_SUSPENDED");
    }
}
