package com.pharmacy.controller;

import com.pharmacy.dto.request.PharmacyCreateRequest;
import com.pharmacy.dto.request.PharmacyUpdateRequest;
import com.pharmacy.dto.request.RegisterPharmacyOwnerRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.dto.response.PharmacyPublicResponse;
import com.pharmacy.dto.response.PharmacyResponse;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.entity.User;
import com.pharmacy.enums.SubscriptionPlan;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.AccessDeniedException;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.mapper.PharmacyMapper;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.AuthService;
import com.pharmacy.service.PharmacyService;
import com.pharmacy.service.ProductService;
import com.pharmacy.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PharmacyController {

    private static final Logger log = LoggerFactory.getLogger(PharmacyController.class);

    private final PharmacyService pharmacyService;
    private final UserService userService;
    private final AuthService authService;
    private final ProductService productService;
    private final PharmacyMapper pharmacyMapper;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public PharmacyController(PharmacyService pharmacyService,
                              UserService userService,
                              AuthService authService,
                              ProductService productService,
                              PharmacyMapper pharmacyMapper,
                              SecurityUtils securityUtils,
                              AuditLogService auditLogService) {
        this.pharmacyService = pharmacyService;
        this.userService = userService;
        this.authService = authService;
        this.productService = productService;
        this.pharmacyMapper = pharmacyMapper;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get pharmacy info by subdomain (for storefront)
     * GET /api/public/pharmacies/subdomain/{subdomain}
     */
    @GetMapping("/public/pharmacies/subdomain/{subdomain}")
    public ResponseEntity<PharmacyPublicResponse> getPharmacyBySubdomain(@PathVariable String subdomain) {
        Pharmacy pharmacy = pharmacyService.findBySubdomain(subdomain.toLowerCase())
                .orElseThrow(() -> new BadRequestException("Pharmacy not found"));

        pharmacyService.validatePharmacyActive(pharmacy.getId());

        return ResponseEntity.ok(pharmacyMapper.toPublicResponse(pharmacy));
    }

    /**
     * Get pharmacy info by custom domain (for storefront)
     * GET /api/public/pharmacies/domain/{domain}
     */
    @GetMapping("/public/pharmacies/domain/{domain}")
    public ResponseEntity<PharmacyPublicResponse> getPharmacyByDomain(@PathVariable String domain) {
        Pharmacy pharmacy = pharmacyService.findByCustomDomain(domain)
                .orElseThrow(() -> new BadRequestException("Pharmacy not found"));

        pharmacyService.validatePharmacyActive(pharmacy.getId());

        return ResponseEntity.ok(pharmacyMapper.toPublicResponse(pharmacy));
    }

    /**
     * Get pharmacy public info by ID
     * GET /api/public/pharmacies/{id}
     */
    @GetMapping("/public/pharmacies/{id}")
    public ResponseEntity<PharmacyPublicResponse> getPharmacyPublic(@PathVariable Long id) {
        Pharmacy pharmacy = pharmacyService.getById(id);
        pharmacyService.validatePharmacyActive(id);

        return ResponseEntity.ok(pharmacyMapper.toPublicResponse(pharmacy));
    }

    // ==================== ADMIN ENDPOINTS (Super Admin Only) ====================

    /**
     * Get all pharmacies
     * GET /api/admin/pharmacies
     */
    @GetMapping("/admin/pharmacies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<PharmacyResponse>> getAllPharmacies() {
        List<Pharmacy> pharmacies = pharmacyService.findAll();
        List<PharmacyResponse> responses = pharmacies.stream()
                .map(pharmacyMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get pharmacy by ID (admin view with full details)
     * GET /api/admin/pharmacies/{id}
     */
    @GetMapping("/admin/pharmacies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PharmacyResponse> getPharmacy(@PathVariable Long id) {
        Pharmacy pharmacy = pharmacyService.getById(id);
        PharmacyResponse response = pharmacyMapper.toResponse(pharmacy);

        // Add stats
        response.setProductCount(productService.countByPharmacy(id));

        return ResponseEntity.ok(response);
    }

    /**
     * Create new pharmacy with owner
     * POST /api/admin/pharmacies
     */
    @PostMapping("/admin/pharmacies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> createPharmacy(
            @Valid @RequestBody PharmacyCreateRequest request) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        // Validate subdomain
        if (!pharmacyService.isSubdomainAvailable(request.getSubdomain())) {
            throw new DuplicateResourceException("Pharmacy", "subdomain", request.getSubdomain());
        }

        // Validate custom domain if provided
        if (request.getCustomDomain() != null && !request.getCustomDomain().isEmpty()) {
            if (!pharmacyService.isCustomDomainAvailable(request.getCustomDomain())) {
                throw new DuplicateResourceException("Pharmacy", "customDomain", request.getCustomDomain());
            }
        }

        // Validate owner email
        if (userService.emailExists(request.getOwnerEmail())) {
            throw new DuplicateResourceException("User", "email", request.getOwnerEmail());
        }

        // Create pharmacy
        Pharmacy pharmacy = pharmacyMapper.toEntity(request);
        pharmacy = pharmacyService.createPharmacy(pharmacy);

        // Create owner
        RegisterPharmacyOwnerRequest ownerRequest = new RegisterPharmacyOwnerRequest();
        ownerRequest.setPharmacyId(pharmacy.getId());
        ownerRequest.setEmail(request.getOwnerEmail());
        ownerRequest.setPassword(request.getOwnerPassword());
        ownerRequest.setFirstName(request.getOwnerFirstName());
        ownerRequest.setLastName(request.getOwnerLastName());
        ownerRequest.setPhone(request.getOwnerPhone());

        User owner = authService.registerPharmacyOwner(ownerRequest, adminId, adminEmail);

        // Audit log
        auditLogService.logPharmacyCreated(adminId, adminEmail, pharmacy.getId(), pharmacy.getName(), pharmacy.getSubscriptionPlan().name());

        log.info("Pharmacy created: {} with owner: {} by admin: {}",
                pharmacy.getName(), owner.getEmail(), adminEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pharmacy created successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    /**
     * Update pharmacy (admin)
     * PUT /api/admin/pharmacies/{id}
     */
    @PutMapping("/admin/pharmacies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> updatePharmacyAdmin(
            @PathVariable Long id,
            @Valid @RequestBody PharmacyUpdateRequest request) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.getById(id);

        // Validate custom domain change
        if (request.getCustomDomain() != null &&
                !request.getCustomDomain().equals(pharmacy.getCustomDomain())) {
            if (!pharmacyService.isCustomDomainAvailable(request.getCustomDomain())) {
                throw new DuplicateResourceException("Pharmacy", "customDomain", request.getCustomDomain());
            }
        }

        pharmacyMapper.updateEntity(pharmacy, request);
        pharmacy = pharmacyService.updatePharmacy(pharmacy);

        // Audit log
        auditLogService.logPharmacyUpdated(pharmacy.getId(), adminId, adminEmail, null, "Pharmacy updated by admin");

        log.info("Pharmacy updated by admin: {} - {}", pharmacy.getName(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy updated successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    /**
     * Upgrade pharmacy plan
     * PATCH /api/admin/pharmacies/{id}/upgrade
     */
    @PatchMapping("/admin/pharmacies/{id}/upgrade")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> upgradePlan(
            @PathVariable Long id,
            @RequestParam SubscriptionPlan plan) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.upgradePlan(id, plan);

        // Audit log
        auditLogService.logPharmacyPlanUpgraded(pharmacy.getId(), adminId, adminEmail, null, plan.name());

        log.info("Pharmacy plan upgraded: {} to {} by admin: {}", pharmacy.getName(), plan, adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Plan upgraded successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    /**
     * Suspend pharmacy
     * PATCH /api/admin/pharmacies/{id}/suspend
     */
    @PatchMapping("/admin/pharmacies/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> suspendPharmacy(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.suspendPharmacy(id);

        // Audit log
        auditLogService.logPharmacySuspended(adminId, adminEmail, pharmacy.getId(), pharmacy.getName(), "Suspended by admin");

        log.info("Pharmacy suspended: {} by admin: {}", pharmacy.getName(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy suspended", pharmacyMapper.toResponse(pharmacy)));
    }

    /**
     * Reactivate pharmacy
     * PATCH /api/admin/pharmacies/{id}/reactivate
     */
    @PatchMapping("/admin/pharmacies/{id}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> reactivatePharmacy(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.reactivatePharmacy(id);

        // Audit log
        auditLogService.logPharmacyActivated(adminId, adminEmail, pharmacy.getId(), pharmacy.getName());

        log.info("Pharmacy reactivated: {} by admin: {}", pharmacy.getName(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy reactivated", pharmacyMapper.toResponse(pharmacy)));
    }

    /**
     * Check subdomain availability
     * GET /api/admin/pharmacies/check-subdomain?subdomain=xxx
     */
    @GetMapping("/admin/pharmacies/check-subdomain")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkSubdomainAvailability(@RequestParam String subdomain) {
        boolean available = pharmacyService.isSubdomainAvailable(subdomain.toLowerCase());
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Check custom domain availability
     * GET /api/admin/pharmacies/check-domain?domain=xxx
     */
    @GetMapping("/admin/pharmacies/check-domain")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Boolean>> checkDomainAvailability(@RequestParam String domain) {
        boolean available = pharmacyService.isCustomDomainAvailable(domain);
        return ResponseEntity.ok(Map.of("available", available));
    }

    // ==================== PHARMACY OWNER ENDPOINTS ====================

    /**
     * Get own pharmacy info
     * GET /api/pharmacy/info
     */
    @GetMapping("/pharmacy/info")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    public ResponseEntity<PharmacyResponse> getOwnPharmacy() {
        Long pharmacyId = getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);

        PharmacyResponse response = pharmacyMapper.toResponse(pharmacy);
        response.setProductCount(productService.countByPharmacy(pharmacyId));

        return ResponseEntity.ok(response);
    }

    /**
     * Update own pharmacy
     * PUT /api/pharmacy/info
     */
    @PutMapping("/pharmacy/info")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    public ResponseEntity<ApiResponse<PharmacyResponse>> updateOwnPharmacy(
            @Valid @RequestBody PharmacyUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);

        // Validate custom domain change
        if (request.getCustomDomain() != null &&
                !request.getCustomDomain().equals(pharmacy.getCustomDomain())) {
            if (!pharmacyService.isCustomDomainAvailable(request.getCustomDomain())) {
                throw new DuplicateResourceException("Pharmacy", "customDomain", request.getCustomDomain());
            }
        }

        pharmacyMapper.updateEntity(pharmacy, request);
        pharmacy = pharmacyService.updatePharmacy(pharmacy);

        // Audit log
        auditLogService.logPharmacyUpdated(pharmacy.getId(), userId, userEmail, null, "Updated by owner");

        log.info("Pharmacy updated by owner: {} - {}", pharmacy.getName(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy updated successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    /**
     * Get pharmacy staff list
     * GET /api/pharmacy/staff
     */
    @GetMapping("/pharmacy/staff")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    public ResponseEntity<List<Map<String, Object>>> getPharmacyStaff() {
        Long pharmacyId = getCurrentPharmacyId();

        List<User> staff = userService.findStaffByPharmacy(pharmacyId);

        List<Map<String, Object>> responses = staff.stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName(),
                        "phone", user.getPhone() != null ? user.getPhone() : "",
                        "role", user.getRole().name(),
                        "active", user.isActive(),
                        "lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : ""
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Deactivate staff member
     * PATCH /api/pharmacy/staff/{staffId}/deactivate
     */
    @PatchMapping("/pharmacy/staff/{staffId}/deactivate")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    public ResponseEntity<ApiResponse<String>> deactivateStaff(@PathVariable Long staffId) {
        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        User staff = userService.getById(staffId);

        // Validate staff belongs to pharmacy
        if (staff.getPharmacy() == null || !staff.getPharmacy().getId().equals(pharmacyId)) {
            throw AccessDeniedException.resourceAccess("staff");
        }

        // Cannot deactivate owner
        if (staff.getRole() == UserRole.PHARMACY_OWNER) {
            throw new BadRequestException("Cannot deactivate pharmacy owner");
        }

        userService.deactivateUser(staffId);

        auditLogService.logUserDeactivated(pharmacyId, userId, userEmail, staffId, staff.getEmail());

        return ResponseEntity.ok(ApiResponse.success("Staff member deactivated"));
    }

    /**
     * Activate staff member
     * PATCH /api/pharmacy/staff/{staffId}/activate
     */
    @PatchMapping("/pharmacy/staff/{staffId}/activate")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    public ResponseEntity<ApiResponse<String>> activateStaff(@PathVariable Long staffId) {
        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        User staff = userService.getById(staffId);

        // Validate staff belongs to pharmacy
        if (staff.getPharmacy() == null || !staff.getPharmacy().getId().equals(pharmacyId)) {
            throw AccessDeniedException.resourceAccess("staff");
        }

        userService.activateUser(staffId);

        auditLogService.logUserActivated(pharmacyId, userId, userEmail, staffId, staff.getEmail());

        return ResponseEntity.ok(ApiResponse.success("Staff member activated"));
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentPharmacyId() {
        return securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));
    }
}