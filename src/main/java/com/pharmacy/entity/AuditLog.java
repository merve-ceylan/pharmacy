package com.pharmacy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_pharmacy", columnList = "pharmacy_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action_type"),
    @Index(name = "idx_audit_entity", columnList = "entity_type"),
    @Index(name = "idx_audit_created", columnList = "created_at")
})
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pharmacy_id")
    private Long pharmacyId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "user_email")
    private String userEmail;
    
    @Column(name = "action_type", nullable = false)
    private String actionType;  // CREATE, UPDATE, DELETE, LOGIN, LOGOUT, etc.
    
    @Column(name = "entity_type", nullable = false)
    private String entityType;  // USER, PRODUCT, ORDER, PHARMACY, etc.
    
    @Column(name = "entity_id")
    private Long entityId;
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;  // JSON
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;  // JSON
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "request_url")
    private String requestUrl;
    
    @Column(name = "request_method")
    private String requestMethod;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getPharmacyId() {
        return pharmacyId;
    }
    
    public void setPharmacyId(Long pharmacyId) {
        this.pharmacyId = pharmacyId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getActionType() {
        return actionType;
    }
    
    public void setActionType(String actionType) {
        this.actionType = actionType;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public Long getEntityId() {
        return entityId;
    }
    
    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
    
    public String getOldValue() {
        return oldValue;
    }
    
    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getRequestUrl() {
        return requestUrl;
    }
    
    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }
    
    public String getRequestMethod() {
        return requestMethod;
    }
    
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Builder pattern for easy creation
    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }
    
    public static class AuditLogBuilder {
        private final AuditLog log = new AuditLog();
        
        public AuditLogBuilder pharmacyId(Long pharmacyId) {
            log.setPharmacyId(pharmacyId);
            return this;
        }
        
        public AuditLogBuilder userId(Long userId) {
            log.setUserId(userId);
            return this;
        }
        
        public AuditLogBuilder userEmail(String userEmail) {
            log.setUserEmail(userEmail);
            return this;
        }
        
        public AuditLogBuilder actionType(String actionType) {
            log.setActionType(actionType);
            return this;
        }
        
        public AuditLogBuilder entityType(String entityType) {
            log.setEntityType(entityType);
            return this;
        }
        
        public AuditLogBuilder entityId(Long entityId) {
            log.setEntityId(entityId);
            return this;
        }
        
        public AuditLogBuilder oldValue(String oldValue) {
            log.setOldValue(oldValue);
            return this;
        }
        
        public AuditLogBuilder newValue(String newValue) {
            log.setNewValue(newValue);
            return this;
        }
        
        public AuditLogBuilder description(String description) {
            log.setDescription(description);
            return this;
        }
        
        public AuditLogBuilder ipAddress(String ipAddress) {
            log.setIpAddress(ipAddress);
            return this;
        }
        
        public AuditLogBuilder userAgent(String userAgent) {
            log.setUserAgent(userAgent);
            return this;
        }
        
        public AuditLogBuilder requestUrl(String requestUrl) {
            log.setRequestUrl(requestUrl);
            return this;
        }
        
        public AuditLogBuilder requestMethod(String requestMethod) {
            log.setRequestMethod(requestMethod);
            return this;
        }
        
        public AuditLog build() {
            return log;
        }
    }
}
