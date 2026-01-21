package com.pharmacy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "settings")
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Platform Settings
    @Column(name = "platform_name", nullable = false)
    private String platformName;

    @Column(name = "platform_email", nullable = false)
    private String platformEmail;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "support_phone")
    private String supportPhone;

    // Business Rules
    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 7;

    @Column(name = "data_retention_days", nullable = false)
    private Integer dataRetentionDays = 30;

    @Column(name = "max_failed_login_attempts", nullable = false)
    private Integer maxFailedLoginAttempts = 5;

    @Column(name = "account_lockout_minutes", nullable = false)
    private Integer accountLockoutMinutes = 30;

    // Email Settings
    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "email_from_name")
    private String emailFromName;

    @Column(name = "email_from_address")
    private String emailFromAddress;

    // Subscription Settings
    @Column(name = "trial_period_days")
    private Integer trialPeriodDays = 14;

    @Column(name = "allow_custom_domains", nullable = false)
    private Boolean allowCustomDomains = true;

    // Feature Flags
    @Column(name = "maintenance_mode", nullable = false)
    private Boolean maintenanceMode = false;

    @Column(name = "registrations_enabled", nullable = false)
    private Boolean registrationsEnabled = true;

    @Column(name = "email_verification_required", nullable = false)
    private Boolean emailVerificationRequired = false;

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Settings() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformEmail() {
        return platformEmail;
    }

    public void setPlatformEmail(String platformEmail) {
        this.platformEmail = platformEmail;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
    }

    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        this.dataRetentionDays = dataRetentionDays;
    }

    public Integer getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public void setMaxFailedLoginAttempts(Integer maxFailedLoginAttempts) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    public Integer getAccountLockoutMinutes() {
        return accountLockoutMinutes;
    }

    public void setAccountLockoutMinutes(Integer accountLockoutMinutes) {
        this.accountLockoutMinutes = accountLockoutMinutes;
    }

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public String getEmailFromName() {
        return emailFromName;
    }

    public void setEmailFromName(String emailFromName) {
        this.emailFromName = emailFromName;
    }

    public String getEmailFromAddress() {
        return emailFromAddress;
    }

    public void setEmailFromAddress(String emailFromAddress) {
        this.emailFromAddress = emailFromAddress;
    }

    public Integer getTrialPeriodDays() {
        return trialPeriodDays;
    }

    public void setTrialPeriodDays(Integer trialPeriodDays) {
        this.trialPeriodDays = trialPeriodDays;
    }

    public Boolean getAllowCustomDomains() {
        return allowCustomDomains;
    }

    public void setAllowCustomDomains(Boolean allowCustomDomains) {
        this.allowCustomDomains = allowCustomDomains;
    }

    public Boolean getMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(Boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public Boolean getRegistrationsEnabled() {
        return registrationsEnabled;
    }

    public void setRegistrationsEnabled(Boolean registrationsEnabled) {
        this.registrationsEnabled = registrationsEnabled;
    }

    public Boolean getEmailVerificationRequired() {
        return emailVerificationRequired;
    }

    public void setEmailVerificationRequired(Boolean emailVerificationRequired) {
        this.emailVerificationRequired = emailVerificationRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}