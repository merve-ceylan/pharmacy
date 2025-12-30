package com.pharmacy.controller;

import com.pharmacy.dto.common.UserDto;
import com.pharmacy.dto.request.*;
import com.pharmacy.dto.response.AuthResponse;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.securityUtils = securityUtils;
    }

    /**
     * Login endpoint
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    /**
     * Register new customer
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerCustomer(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.registerCustomer(request, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Register pharmacy owner (Super Admin only)
     * POST /api/auth/register/pharmacy-owner
     */
    @PostMapping("/register/pharmacy-owner")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> registerPharmacyOwner(
            @Valid @RequestBody RegisterPharmacyOwnerRequest request) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        var user = authService.registerPharmacyOwner(request, adminId, adminEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Pharmacy owner created successfully",
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    /**
     * Register staff (Pharmacy Owner only)
     * POST /api/auth/register/staff
     */
    @PostMapping("/register/staff")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    public ResponseEntity<Map<String, Object>> registerStaff(
            @Valid @RequestBody RegisterStaffRequest request) {

        Long ownerId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String ownerEmail = securityUtils.getCurrentUserEmail().orElse("unknown");
        Long pharmacyId = securityUtils.getCurrentPharmacyId()
                .orElseThrow(() -> new BadRequestException("Pharmacy not found for user"));

        var user = authService.registerStaff(request, ownerId, ownerEmail, pharmacyId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Staff member created successfully",
                "userId", user.getId(),
                "email", user.getEmail()
        ));
    }

    /**
     * Logout - invalidate token
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest httpRequest) {
        String authHeader = httpRequest.getHeader("Authorization");
        String ipAddress = SecurityUtils.getClientIP(httpRequest);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = securityUtils.getCurrentUserId().orElse(null);
            String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");

            authService.logout(token, userId, userEmail, ipAddress);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    /**
     * Refresh access token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    /**
     * Get current user info
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        Long userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));

        UserDto user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Change password
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        // Validate password match
        if (!request.isPasswordMatch()) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        Long userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("User not authenticated"));
        String ipAddress = SecurityUtils.getClientIP(httpRequest);

        authService.changePassword(request, userId, ipAddress);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully"
        ));
    }

    /**
     * Request password reset
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIP(httpRequest);

        authService.requestPasswordReset(request.getEmail(), ipAddress);

        // Always return success to prevent user enumeration
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "If the email exists, a password reset link has been sent"
        ));
    }

    /**
     * Check if email is available
     * GET /api/auth/check-email?email=xxx
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        // This endpoint intentionally doesn't reveal if email exists
        // Just validates format
        boolean validFormat = email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

        return ResponseEntity.ok(Map.of(
                "valid", validFormat
        ));
    }

    /**
     * Health check for auth service
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "auth"
        ));
    }
}
