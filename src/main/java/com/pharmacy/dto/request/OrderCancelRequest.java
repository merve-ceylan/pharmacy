package com.pharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OrderCancelRequest {

    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    // Getters and Setters
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
