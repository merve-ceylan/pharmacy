package com.pharmacy.controller;

import com.pharmacy.dto.response.ApiResponse;
import com.pharmacy.entity.User;
import com.pharmacy.enums.UserRole;

import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuditLogService;
import com.pharmacy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "User Management", description = "User management endpoints (Super Admin)")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    public UserController(UserService userService,
                          SecurityUtils securityUtils,
                          AuditLogService auditLogService) {
        this.userService = userService;
        this.securityUtils = securityUtils;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "List all users",
            description = "Get all users in the system (Super Admin only)",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<List<Map<String, Object>>> getAllUsers(
            @Parameter(description = "Filter by role") @RequestParam(required = false) UserRole role) {

        List<User> users = role != null
                ? userService.findByRole(role)
                : userService.findAll();

        List<Map<String, Object>> responses = users.stream()
                .map(user -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", user.getId());
                    map.put("email", user.getEmail());
                    map.put("firstName", user.getFirstName());
                    map.put("lastName", user.getLastName());
                    map.put("phone", user.getPhone() != null ? user.getPhone() : "");
                    map.put("role", user.getRole().name());
                    map.put("active", user.isActive());
                    map.put("emailVerified", user.isEmailVerified());
                    map.put("pharmacyId", user.getPharmacy() != null ? user.getPharmacy().getId() : null);
                    map.put("pharmacyName", user.getPharmacy() != null ? user.getPharmacy().getName() : null);
                    map.put("createdAt", user.getCreatedAt().toString());
                    map.put("lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null);
                    return map;
                })
                .toList();

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Get user by ID",
            description = "Get detailed user information",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long id) {
        User user = userService.getById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("phone", user.getPhone() != null ? user.getPhone() : "");
        response.put("role", user.getRole().name());
        response.put("active", user.isActive());
        response.put("emailVerified", user.isEmailVerified());
        response.put("pharmacyId", user.getPharmacy() != null ? user.getPharmacy().getId() : null);
        response.put("pharmacyName", user.getPharmacy() != null ? user.getPharmacy().getName() : null);
        response.put("createdAt", user.getCreatedAt().toString());
        response.put("lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null);
        response.put("failedLoginAttempts", user.getFailedLoginAttempts());
        response.put("lockedUntil", user.getLockedUntil() != null ? user.getLockedUntil().toString() : null);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Activate user",
            description = "Activate a user account",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<String>> activateUser(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        User user = userService.getById(id);
        userService.activateUser(id);

        Long pharmacyId = user.getPharmacy() != null ? user.getPharmacy().getId() : null;
        auditLogService.logUserActivated(pharmacyId, adminId, adminEmail, id, user.getEmail());

        log.info("User activated: {} by admin: {}", user.getEmail(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("User activated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Deactivate user",
            description = "Deactivate a user account",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<String>> deactivateUser(@PathVariable Long id) {
        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        User user = userService.getById(id);
        userService.deactivateUser(id);

        Long pharmacyId = user.getPharmacy() != null ? user.getPharmacy().getId() : null;
        auditLogService.logUserDeactivated(pharmacyId, adminId, adminEmail, id, user.getEmail());

        log.info("User deactivated: {} by admin: {}", user.getEmail(), adminEmail);

        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully"));
    }


}