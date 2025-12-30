package com.pharmacy.entity;

import com.pharmacy.enums.DeliveryType;
import com.pharmacy.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_number", columnList = "order_number")
})
public class Order extends BaseEntity {

    // Multi-tenant: which pharmacy this order belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    // Customer who placed the order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    // Unique order number: ORD-2025-00001
    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Order items (products in this order)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // Price fields - captured at order time
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "shipping_cost", precision = 10, scale = 2)
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // Delivery information
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false)
    private DeliveryType deliveryType;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "cargo_company")
    private String cargoCompany;

    // Shipping address
    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "shipping_city", nullable = false)
    private String shippingCity;

    @Column(name = "shipping_district")
    private String shippingDistrict;

    @Column(name = "shipping_postal_code")
    private String shippingPostalCode;

    @Column(name = "shipping_phone", nullable = false)
    private String shippingPhone;

    // Order notes from customer
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Cancellation info
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    // Timestamps for status tracking
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "preparing_at")
    private LocalDateTime preparingAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // Getters and Setters
    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public DeliveryType getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(DeliveryType deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCargoCompany() {
        return cargoCompany;
    }

    public void setCargoCompany(String cargoCompany) {
        this.cargoCompany = cargoCompany;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingDistrict() {
        return shippingDistrict;
    }

    public void setShippingDistrict(String shippingDistrict) {
        this.shippingDistrict = shippingDistrict;
    }

    public String getShippingPostalCode() {
        return shippingPostalCode;
    }

    public void setShippingPostalCode(String shippingPostalCode) {
        this.shippingPostalCode = shippingPostalCode;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Long getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(Long cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public LocalDateTime getPreparingAt() {
        return preparingAt;
    }

    public void setPreparingAt(LocalDateTime preparingAt) {
        this.preparingAt = preparingAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    // Helper methods
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }

    public boolean isCompleted() {
        return status == OrderStatus.DELIVERED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public int getTotalItemCount() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }
}