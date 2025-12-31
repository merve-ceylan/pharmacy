package com.pharmacy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pharmacy.enums.PaymentPeriod;
import com.pharmacy.enums.PharmacyStatus;
import com.pharmacy.enums.SubscriptionPlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PharmacyResponse {

    private Long id;
    private String name;
    private String subdomain;
    private String customDomain;
    private String fullUrl;

    // Contact info
    private String address;
    private String city;
    private String district;
    private String postalCode;
    private String phone;
    private String email;

    // Business info
    private String taxNumber;
    private String taxOffice;
    private String glnNumber;

    // Branding
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;

    // Subscription info
    private SubscriptionPlan subscriptionPlan;
    private PaymentPeriod paymentPeriod;
    private BigDecimal setupFee;
    private BigDecimal monthlyFee;
    private LocalDate subscriptionStartDate;
    private LocalDate nextPaymentDate;
    private LocalDate gracePeriodEnd;

    // Status
    private PharmacyStatus status;
    private LocalDateTime suspendedAt;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Stats (optional)
    private Long productCount;
    private Long orderCount;
    private Long customerCount;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public String getFullUrl() { return fullUrl; }
    public void setFullUrl(String fullUrl) { this.fullUrl = fullUrl; }

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

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

    public SubscriptionPlan getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public PaymentPeriod getPaymentPeriod() { return paymentPeriod; }
    public void setPaymentPeriod(PaymentPeriod paymentPeriod) { this.paymentPeriod = paymentPeriod; }

    public BigDecimal getSetupFee() { return setupFee; }
    public void setSetupFee(BigDecimal setupFee) { this.setupFee = setupFee; }

    public BigDecimal getMonthlyFee() { return monthlyFee; }
    public void setMonthlyFee(BigDecimal monthlyFee) { this.monthlyFee = monthlyFee; }

    public LocalDate getSubscriptionStartDate() { return subscriptionStartDate; }
    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) { this.subscriptionStartDate = subscriptionStartDate; }

    public LocalDate getNextPaymentDate() { return nextPaymentDate; }
    public void setNextPaymentDate(LocalDate nextPaymentDate) { this.nextPaymentDate = nextPaymentDate; }

    public LocalDate getGracePeriodEnd() { return gracePeriodEnd; }
    public void setGracePeriodEnd(LocalDate gracePeriodEnd) { this.gracePeriodEnd = gracePeriodEnd; }

    public PharmacyStatus getStatus() { return status; }
    public void setStatus(PharmacyStatus status) { this.status = status; }

    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(LocalDateTime suspendedAt) { this.suspendedAt = suspendedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getProductCount() { return productCount; }
    public void setProductCount(Long productCount) { this.productCount = productCount; }

    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }

    public Long getCustomerCount() { return customerCount; }
    public void setCustomerCount(Long customerCount) { this.customerCount = customerCount; }
}