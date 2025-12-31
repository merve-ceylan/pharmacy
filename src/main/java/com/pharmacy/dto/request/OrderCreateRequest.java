package com.pharmacy.dto.request;

import com.pharmacy.enums.DeliveryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderCreateRequest {

    @NotNull(message = "Pharmacy ID is required")
    private Long pharmacyId;

    @NotNull(message = "Delivery type is required")
    private DeliveryType deliveryType;

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String shippingAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String shippingCity;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String shippingDistrict;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String shippingPostalCode;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String shippingPhone;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    // Getters and Setters
    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }

    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType deliveryType) { this.deliveryType = deliveryType; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingDistrict() { return shippingDistrict; }
    public void setShippingDistrict(String shippingDistrict) { this.shippingDistrict = shippingDistrict; }

    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }

    public String getShippingPhone() { return shippingPhone; }
    public void setShippingPhone(String shippingPhone) { this.shippingPhone = shippingPhone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
