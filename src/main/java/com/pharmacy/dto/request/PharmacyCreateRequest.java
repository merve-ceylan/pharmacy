package com.pharmacy.dto.request;

import com.pharmacy.enums.PaymentPeriod;
import com.pharmacy.enums.SubscriptionPlan;
import jakarta.validation.constraints.*;

public class PharmacyCreateRequest {

    @NotBlank(message = "Pharmacy name is required")
    @Size(min = 2, max = 200, message = "Pharmacy name must be between 2 and 200 characters")
    private String name;

    @NotBlank(message = "Subdomain is required")
    @Size(min = 3, max = 50, message = "Subdomain must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain can only contain lowercase letters, numbers and hyphens")
    private String subdomain;

    @Size(max = 100, message = "Custom domain cannot exceed 100 characters")
    private String customDomain;

    @NotBlank(message = "Owner first name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String ownerFirstName;

    @NotBlank(message = "Owner last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String ownerLastName;

    @NotBlank(message = "Owner email is required")
    @Email(message = "Invalid email format")
    private String ownerEmail;

    @NotBlank(message = "Owner password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String ownerPassword;

    @NotBlank(message = "Owner phone is required")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String ownerPhone;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "District is required")
    @Size(max = 100, message = "District cannot exceed 100 characters")
    private String district;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 20, message = "Tax number cannot exceed 20 characters")
    private String taxNumber;

    @Size(max = 200, message = "Tax office cannot exceed 200 characters")
    private String taxOffice;

    @Size(max = 50, message = "GLN number cannot exceed 50 characters")
    private String glnNumber;

    @NotNull(message = "Subscription plan is required")
    private SubscriptionPlan subscriptionPlan;

    @NotNull(message = "Payment period is required")
    private PaymentPeriod paymentPeriod;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public String getOwnerFirstName() { return ownerFirstName; }
    public void setOwnerFirstName(String ownerFirstName) { this.ownerFirstName = ownerFirstName; }

    public String getOwnerLastName() { return ownerLastName; }
    public void setOwnerLastName(String ownerLastName) { this.ownerLastName = ownerLastName; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getOwnerPassword() { return ownerPassword; }
    public void setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; }

    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getTaxOffice() { return taxOffice; }
    public void setTaxOffice(String taxOffice) { this.taxOffice = taxOffice; }

    public String getGlnNumber() { return glnNumber; }
    public void setGlnNumber(String glnNumber) { this.glnNumber = glnNumber; }

    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public PaymentPeriod getPaymentPeriod() { return paymentPeriod; }
    public void setPaymentPeriod(PaymentPeriod paymentPeriod) { this.paymentPeriod = paymentPeriod; }
}