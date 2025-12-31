package com.pharmacy.entity;

import com.pharmacy.enums.PaymentPeriod;
import com.pharmacy.enums.PharmacyStatus;
import com.pharmacy.enums.SubscriptionPlan;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pharmacies")
public class Pharmacy extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "subdomain", unique = true)
    private String subdomain;  // ozan.pharmacyplatform.com

    @Column(name = "custom_domain", unique = true)
    private String customDomain;  // ozaneczanesi.com (Pro only)

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "primary_color")
    private String primaryColor;  // Hex color: #1B4F72

    @Column(name = "secondary_color")
    private String secondaryColor;  // Pro only

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String address;

    private String city;

    private String district;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "tax_number")
    private String taxNumber;

    @Column(name = "tax_office")
    private String taxOffice;

    @Column(name = "gln_number")
    private String glnNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_plan", nullable = false)
    private SubscriptionPlan subscriptionPlan = SubscriptionPlan.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_period", nullable = false)
    private PaymentPeriod paymentPeriod = PaymentPeriod.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PharmacyStatus status = PharmacyStatus.ACTIVE;

    @Column(name = "setup_fee", precision = 10, scale = 2)
    private BigDecimal setupFee;

    @Column(name = "monthly_fee", precision = 10, scale = 2)
    private BigDecimal monthlyFee;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Column(name = "next_payment_date")
    private LocalDate nextPaymentDate;

    @Column(name = "grace_period_end")
    private LocalDate gracePeriodEnd;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getTaxNumber() {
        return taxNumber;
    }

    public void setTaxNumber(String taxNumber) {
        this.taxNumber = taxNumber;
    }

    public String getTaxOffice() {
        return taxOffice;
    }

    public void setTaxOffice(String taxOffice) {
        this.taxOffice = taxOffice;
    }

    public String getGlnNumber() {
        return glnNumber;
    }

    public void setGlnNumber(String glnNumber) {
        this.glnNumber = glnNumber;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(SubscriptionPlan subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public PaymentPeriod getPaymentPeriod() {
        return paymentPeriod;
    }

    public void setPaymentPeriod(PaymentPeriod paymentPeriod) {
        this.paymentPeriod = paymentPeriod;
    }

    public PharmacyStatus getStatus() {
        return status;
    }

    public void setStatus(PharmacyStatus status) {
        this.status = status;
    }

    public BigDecimal getSetupFee() {
        return setupFee;
    }

    public void setSetupFee(BigDecimal setupFee) {
        this.setupFee = setupFee;
    }

    public BigDecimal getMonthlyFee() {
        return monthlyFee;
    }

    public void setMonthlyFee(BigDecimal monthlyFee) {
        this.monthlyFee = monthlyFee;
    }

    public LocalDate getSubscriptionStartDate() {
        return subscriptionStartDate;
    }

    public void setSubscriptionStartDate(LocalDate subscriptionStartDate) {
        this.subscriptionStartDate = subscriptionStartDate;
    }

    public LocalDate getSubscriptionEndDate() {
        return subscriptionEndDate;
    }

    public void setSubscriptionEndDate(LocalDate subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public LocalDate getNextPaymentDate() {
        return nextPaymentDate;
    }

    public void setNextPaymentDate(LocalDate nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public LocalDate getGracePeriodEnd() {
        return gracePeriodEnd;
    }

    public void setGracePeriodEnd(LocalDate gracePeriodEnd) {
        this.gracePeriodEnd = gracePeriodEnd;
    }

    public LocalDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(LocalDateTime suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    // Helper methods
    public boolean isActive() {
        return status == PharmacyStatus.ACTIVE;
    }

    public boolean isPro() {
        return subscriptionPlan == SubscriptionPlan.PRO;
    }

    public String getActiveDomain() {
        if (customDomain != null && !customDomain.isEmpty()) {
            return customDomain;
        }
        return subdomain + ".pharmacyplatform.com";
    }
}