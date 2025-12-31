package com.pharmacy.dto.request;

import com.pharmacy.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private OrderStatus status;

    @Size(max = 100, message = "Tracking number cannot exceed 100 characters")
    private String trackingNumber;

    @Size(max = 100, message = "Cargo company cannot exceed 100 characters")
    private String cargoCompany;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;

    // Getters and Setters
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCargoCompany() { return cargoCompany; }
    public void setCargoCompany(String cargoCompany) { this.cargoCompany = cargoCompany; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
