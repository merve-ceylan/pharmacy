package com.pharmacy.entity;

import com.pharmacy.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "order_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_transaction", columnList = "transaction_id")
})
public class Payment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // iyzico transaction ID
    @Column(name = "transaction_id")
    private String transactionId;

    // iyzico payment ID
    @Column(name = "payment_id")
    private String paymentId;

    // iyzico conversation ID (for tracking)
    @Column(name = "conversation_id")
    private String conversationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Amount refunded (if any)
    @Column(name = "refunded_amount", precision = 10, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    // Card info (masked)
    @Column(name = "card_last_four")
    private String cardLastFour;

    @Column(name = "card_brand")
    private String cardBrand;

    // Error info (if failed)
    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Timestamps
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    // Getters and Setters
    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void setCardLastFour(String cardLastFour) {
        this.cardLastFour = cardLastFour;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    // Helper methods
    public boolean isSuccessful() {
        return status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isPartiallyRefunded() {
        return refundedAmount != null
                && refundedAmount.compareTo(BigDecimal.ZERO) > 0
                && refundedAmount.compareTo(amount) < 0;
    }

    public BigDecimal getNetAmount() {
        if (refundedAmount == null) {
            return amount;
        }
        return amount.subtract(refundedAmount);
    }
}
