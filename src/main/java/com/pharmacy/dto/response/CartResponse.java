package com.pharmacy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {

    private Long id;
    private Long pharmacyId;
    private String pharmacyName;
    private List<CartItemResponse> items;
    private Integer itemCount;
    private Integer totalQuantity;
    private BigDecimal subtotal;
    private BigDecimal estimatedShipping;
    private BigDecimal estimatedTotal;
    private Boolean hasUnavailableItems;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }

    public String getPharmacyName() { return pharmacyName; }
    public void setPharmacyName(String pharmacyName) { this.pharmacyName = pharmacyName; }

    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getEstimatedShipping() { return estimatedShipping; }
    public void setEstimatedShipping(BigDecimal estimatedShipping) { this.estimatedShipping = estimatedShipping; }

    public BigDecimal getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(BigDecimal estimatedTotal) { this.estimatedTotal = estimatedTotal; }

    public Boolean getHasUnavailableItems() { return hasUnavailableItems; }
    public void setHasUnavailableItems(Boolean hasUnavailableItems) { this.hasUnavailableItems = hasUnavailableItems; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}