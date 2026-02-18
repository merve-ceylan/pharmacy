package com.pharmacy.controller;

import com.pharmacy.dto.request.RegisterStaffRequest;
import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.entity.User;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pharmacy/staff")
@Tag(name = "Pharmacy Staff Management", description = "Staff management endpoints for pharmacy owners")
public class PharmacyStaffController {

    private static final Logger log = LoggerFactory.getLogger(PharmacyStaffController.class);

    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public PharmacyStaffController(UserService userService,
                                   SecurityUtils securityUtils,
                                   AuditLogService auditLogService) {
        this.userService = userService;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "List pharmacy staff",
            description = "Get all staff members of the current pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<Map<String, Object>>> getPharmacyStaff() {
        Long pharmacyId = getCurrentPharmacyId();
        Long currentUserId = getCurrentUserId();

        log.info("Fetching staff for pharmacy: {}", pharmacyId);

        List<User> staffUsers = userService.findStaffByPharmacy(pharmacyId);

        List<Map<String, Object>> staffList = staffUsers.stream()
                .map(this::mapUserToResponse)
                .collect(Collectors.toList());

        auditLogService.log(
                pharmacyId,
                currentUserId,
                securityUtils.getCurrentUserEmail().orElse("unknown"),
                "STAFF_LIST_VIEWED",
                "User",
                null,
                null,
                null,
                "Pharmacy staff list viewed"
        );

        return ResponseEntity.ok(staffList);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Get staff statistics",
            description = "Get staff count statistics for the pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> getStaffStats() {
        Long pharmacyId = getCurrentPharmacyId();

        List<User> allStaff = userService.findStaffByPharmacy(pharmacyId);
        long totalStaff = allStaff.size();
        long activeStaff = allStaff.stream().filter(User::isActive).count();
        long inactiveStaff = totalStaff - activeStaff;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", totalStaff);
        stats.put("active", activeStaff);
        stats.put("inactive", inactiveStaff);

        return ResponseEntity.ok(stats);
    }

    @PostMapping
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Add new staff member",
            description = "Register a new staff member for the pharmacy",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> addStaff(
            @Valid @RequestBody RegisterStaffRequest request
    ) {
        Long pharmacyId = getCurrentPharmacyId();
        Long currentUserId = getCurrentUserId();

        log.info("Adding new staff member: {} for pharmacy: {}", request.getEmail(), pharmacyId);

        // Create new User object
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword());
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setPhone(request.getPhone());

        // Get current user's pharmacy
        User currentUser = userService.findById(currentUserId)
                .orElseThrow(() -> new BadRequestException("Current user not found"));

        // Create staff member
        User newStaff = userService.createStaff(newUser, currentUser.getPharmacy());

        auditLogService.logUserCreated(
                pharmacyId,
                currentUserId,
                securityUtils.getCurrentUserEmail().orElse("unknown"),
                newStaff.getId(),
                newStaff.getEmail(),
                "STAFF"
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Staff member added successfully", mapUserToResponse(newStaff)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Activate staff member",
            description = "Activate a staff member account",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> activateStaff(@PathVariable Long id) {
        Long pharmacyId = getCurrentPharmacyId();
        Long currentUserId = getCurrentUserId();

        User staff = userService.findById(id)
                .orElseThrow(() -> new BadRequestException("Staff member not found"));

        // Security check: ensure staff belongs to current pharmacy
        if (!staff.getPharmacy().getId().equals(pharmacyId)) {
            throw new BadRequestException("You can only manage your own pharmacy staff");
        }

        if (!staff.getRole().equals(UserRole.STAFF)) {
            throw new BadRequestException("User is not a staff member");
        }

        staff.setActive(true);
        User updatedStaff = userService.updateUser(staff);

        auditLogService.log(
                pharmacyId,
                currentUserId,
                securityUtils.getCurrentUserEmail().orElse("unknown"),
                "STAFF_ACTIVATED",
                "User",
                id,
                "false",
                "true",
                "Staff member activated: " + staff.getEmail()
        );

        log.info("Staff member activated: {} by user: {}", staff.getEmail(), currentUserId);

        return ResponseEntity.ok(ApiResponse.success(
                "Staff member activated successfully",
                mapUserToResponse(updatedStaff)
        ));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Deactivate staff member",
            description = "Deactivate a staff member account (e.g., when they leave)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> deactivateStaff(@PathVariable Long id) {
        Long pharmacyId = getCurrentPharmacyId();
        Long currentUserId = getCurrentUserId();

        User staff = userService.findById(id)
                .orElseThrow(() -> new BadRequestException("Staff member not found"));

        // Security check: ensure staff belongs to current pharmacy
        if (!staff.getPharmacy().getId().equals(pharmacyId)) {
            throw new BadRequestException("You can only manage your own pharmacy staff");
        }

        if (!staff.getRole().equals(UserRole.STAFF)) {
            throw new BadRequestException("User is not a staff member");
        }

        staff.setActive(false);
        User updatedStaff = userService.updateUser(staff);

        auditLogService.log(
                pharmacyId,
                currentUserId,
                securityUtils.getCurrentUserEmail().orElse("unknown"),
                "STAFF_DEACTIVATED",
                "User",
                id,
                "true",
                "false",
                "Staff member deactivated: " + staff.getEmail()
        );

        log.info("Staff member deactivated: {} by user: {}", staff.getEmail(), currentUserId);

        return ResponseEntity.ok(ApiResponse.success(
                "Staff member deactivated successfully",
                mapUserToResponse(updatedStaff)
        ));
    }

    // ==================== HELPER METHODS ====================

    private Long getCurrentPharmacyId() {
        return securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("No pharmacy associated with current user"));
    }

    private Long getCurrentUserId() {
        return securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
    }

    private Map<String, Object> mapUserToResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole().name());
        response.put("active", user.isActive());
        response.put("emailVerified", user.isEmailVerified());
        response.put("createdAt", user.getCreatedAt());
        response.put("pharmacyId", user.getPharmacy() != null ? user.getPharmacy().getId() : null);
        response.put("pharmacyName", user.getPharmacy() != null ? user.getPharmacy().getName() : null);
        return response;
    }
}