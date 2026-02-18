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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Pharmacy", description = "Pharmacy management endpoints")
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

    @GetMapping("/public/pharmacies/subdomain/{subdomain}")
    @Operation(
            summary = "Get pharmacy by subdomain",
            description = "Get pharmacy info for storefront (by subdomain)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pharmacy found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Pharmacy not found or inactive")
    })
    public ResponseEntity<PharmacyPublicResponse> getPharmacyBySubdomain(@PathVariable String subdomain) {
        Pharmacy pharmacy = pharmacyService.findBySubdomain(subdomain.toLowerCase())
                .orElseThrow(() -> new BadRequestException("Pharmacy not found"));

        pharmacyService.validatePharmacyActive(pharmacy.getId());

        return ResponseEntity.ok(pharmacyMapper.toPublicResponse(pharmacy));
    }

    @GetMapping("/public/pharmacies/domain/{domain}")
    @Operation(
            summary = "Get pharmacy by custom domain",
            description = "Get pharmacy info for storefront (by custom domain)"
    )
    public ResponseEntity<PharmacyPublicResponse> getPharmacyByDomain(@PathVariable String domain) {
        Pharmacy pharmacy = pharmacyService.findByCustomDomain(domain)
                .orElseThrow(() -> new BadRequestException("Pharmacy not found"));

        pharmacyService.validatePharmacyActive(pharmacy.getId());

        return ResponseEntity.ok(pharmacyMapper.toPublicResponse(pharmacy));
    }

    @GetMapping("/public/pharmacies/{id}")
    @Operation(
            summary = "Get pharmacy by ID",
            description = "Get public pharmacy info by ID"
    )
    public ResponseEntity<PharmacyPublicResponse> getPharmacyPublic(@PathVariable Long id) {
        Pharmacy pharmacy = pharmacyService.getById(id);
        pharmacyService.validatePharmacyActive(id);

        return ResponseEntity.ok(pharmacyMapper.toPublicResponse(pharmacy));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @GetMapping("/admin/pharmacies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "List all pharmacies",
            description = "Get all pharmacies (Super Admin only)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<PharmacyResponse>> getAllPharmacies() {
        List<Pharmacy> pharmacies = pharmacyService.findAll();
        List<PharmacyResponse> responses = pharmacies.stream()
                .map(pharmacyMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/admin/pharmacies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get pharmacy details",
            description = "Get full pharmacy details with stats",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PharmacyResponse> getPharmacy(@PathVariable Long id) {
        Pharmacy pharmacy = pharmacyService.getById(id);
        PharmacyResponse response = pharmacyMapper.toResponse(pharmacy);

        response.setProductCount(productService.countByPharmacy(id));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/pharmacies")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Create pharmacy",
            description = "Create a new pharmacy with owner account",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Pharmacy created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Subdomain or email already exists")
    })
    public ResponseEntity<ApiResponse<PharmacyResponse>> createPharmacy(
            @Valid @RequestBody PharmacyCreateRequest request) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        if (!pharmacyService.isSubdomainAvailable(request.getSubdomain())) {
            throw new DuplicateResourceException("Pharmacy", "subdomain", request.getSubdomain());
        }

        if (request.getCustomDomain() != null && !request.getCustomDomain().isEmpty()) {
            if (!pharmacyService.isCustomDomainAvailable(request.getCustomDomain())) {
                throw new DuplicateResourceException("Pharmacy", "customDomain", request.getCustomDomain());
            }
        }

        if (userService.emailExists(request.getOwnerEmail())) {
            throw new DuplicateResourceException("User", "email", request.getOwnerEmail());
        }

        Pharmacy pharmacy = pharmacyMapper.toEntity(request);
        pharmacy = pharmacyService.createPharmacy(pharmacy);

        RegisterPharmacyOwnerRequest ownerRequest = new RegisterPharmacyOwnerRequest();
        ownerRequest.setPharmacyId(pharmacy.getId());
        ownerRequest.setEmail(request.getOwnerEmail());
        ownerRequest.setPassword(request.getOwnerPassword());
        ownerRequest.setFirstName(request.getOwnerFirstName());
        ownerRequest.setLastName(request.getOwnerLastName());
        ownerRequest.setPhone(request.getOwnerPhone());

        User owner = authService.registerPharmacyOwner(ownerRequest, adminId, adminEmail);

        auditLogService.logPharmacyCreated(adminId, adminEmail, pharmacy.getId(), pharmacy.getName(), pharmacy.getSubscriptionPlan().name());

        log.info("Pharmacy created: {} with owner: {} by admin: {}",
                pharmacy.getName(), owner.getEmail(), adminEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Pharmacy created successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    @PutMapping("/admin/pharmacies/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Update pharmacy (Admin)",
            description = "Update pharmacy details",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PharmacyResponse>> updatePharmacyAdmin(
            @PathVariable Long id,
            @Valid @RequestBody PharmacyUpdateRequest request) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.getById(id);

        if (request.getCustomDomain() != null &&
                !request.getCustomDomain().equals(pharmacy.getCustomDomain())) {
            if (!pharmacyService.isCustomDomainAvailable(request.getCustomDomain())) {
                throw new DuplicateResourceException("Pharmacy", "customDomain", request.getCustomDomain());
            }
        }

        pharmacyMapper.updateEntity(pharmacy, request);
        pharmacy = pharmacyService.updatePharmacy(pharmacy);

        auditLogService.logPharmacyUpdated(pharmacy.getId(), adminId, adminEmail, null, "Pharmacy updated by admin");

        log.info("Pharmacy updated by admin: {} - {}", pharmacy.getName(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy updated successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    @PatchMapping("/admin/pharmacies/{id}/upgrade")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Upgrade subscription plan",
            description = "Upgrade pharmacy to a higher plan",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PharmacyResponse>> upgradePlan(
            @PathVariable Long id,
            @Parameter(description = "New subscription plan") @RequestParam SubscriptionPlan plan) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.upgradePlan(id, plan);

        auditLogService.logPharmacyPlanUpgraded(pharmacy.getId(), adminId, adminEmail, null, plan.name());

        log.info("Pharmacy plan upgraded: {} to {} by admin: {}", pharmacy.getName(), plan, adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Plan upgraded successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    @PatchMapping("/admin/pharmacies/{id}/suspend")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Suspend pharmacy",
            description = "Suspend a pharmacy (disables access)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PharmacyResponse>> suspendPharmacy(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.suspendPharmacy(id);

        auditLogService.logPharmacySuspended(adminId, adminEmail, pharmacy.getId(), pharmacy.getName(), "Suspended by admin");

        log.info("Pharmacy suspended: {} by admin: {}", pharmacy.getName(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy suspended", pharmacyMapper.toResponse(pharmacy)));
    }

    @PatchMapping("/admin/pharmacies/{id}/reactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Reactivate pharmacy",
            description = "Reactivate a suspended pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PharmacyResponse>> reactivatePharmacy(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        Pharmacy pharmacy = pharmacyService.reactivatePharmacy(id);

        auditLogService.logPharmacyActivated(adminId, adminEmail, pharmacy.getId(), pharmacy.getName());

        log.info("Pharmacy reactivated: {} by admin: {}", pharmacy.getName(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy reactivated", pharmacyMapper.toResponse(pharmacy)));
    }

    @GetMapping("/admin/pharmacies/check-subdomain")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Check subdomain availability",
            description = "Check if a subdomain is available",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Boolean>> checkSubdomainAvailability(
            @Parameter(description = "Subdomain to check") @RequestParam String subdomain) {
        boolean available = pharmacyService.isSubdomainAvailable(subdomain.toLowerCase());
        return ResponseEntity.ok(Map.of("available", available));
    }

    @GetMapping("/admin/pharmacies/check-domain")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Check domain availability",
            description = "Check if a custom domain is available",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Boolean>> checkDomainAvailability(
            @Parameter(description = "Domain to check") @RequestParam String domain) {
        boolean available = pharmacyService.isCustomDomainAvailable(domain);
        return ResponseEntity.ok(Map.of("available", available));
    }

    // ==================== PHARMACY OWNER ENDPOINTS ====================

    @GetMapping("/pharmacy/info")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Get own pharmacy",
            description = "Get pharmacy info for the current owner",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<PharmacyResponse> getOwnPharmacy() {
        Long pharmacyId = getCurrentPharmacyId();
        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);

        PharmacyResponse response = pharmacyMapper.toResponse(pharmacy);
        response.setProductCount(productService.countByPharmacy(pharmacyId));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/pharmacy/info")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Update own pharmacy",
            description = "Update pharmacy details (owner only)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<PharmacyResponse>> updateOwnPharmacy(
            @Valid @RequestBody PharmacyUpdateRequest request) {

        Long pharmacyId = getCurrentPharmacyId();
        Long userId = securityUtils.getCurrentUserId().orElse(null);
        String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

        Pharmacy pharmacy = pharmacyService.getById(pharmacyId);

        if (request.getCustomDomain() != null &&
                !request.getCustomDomain().equals(pharmacy.getCustomDomain())) {
            if (!pharmacyService.isCustomDomainAvailable(request.getCustomDomain())) {
                throw new DuplicateResourceException("Pharmacy", "customDomain", request.getCustomDomain());
            }
        }

        pharmacyMapper.updateEntity(pharmacy, request);
        pharmacy = pharmacyService.updatePharmacy(pharmacy);

        auditLogService.logPharmacyUpdated(pharmacy.getId(), userId, userEmail, null, "Updated by owner");

        log.info("Pharmacy updated by owner: {} - {}", pharmacy.getName(), userEmail);

        return ResponseEntity.ok(ApiResponse.success("Pharmacy updated successfully", pharmacyMapper.toResponse(pharmacy)));
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentPharmacyId() {
        return securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));
    }
}