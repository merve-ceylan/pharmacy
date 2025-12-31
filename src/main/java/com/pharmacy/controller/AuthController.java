package com.pharmacy.controller;

import com.pharmacy.dto.request.*;
import com.pharmacy.dto.response.AuthResponse;
import com.pharmacy.dto.common.UserDto;
import com.pharmacy.entity.User;
import com.pharmacy.security.SecurityUtils;
import com.pharmacy.service.AuthService;
import com.pharmacy.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final SecurityUtils securityUtils;

    public AuthController(AuthService authService, UserService userService, SecurityUtils securityUtils) {
        this.authService = authService;
        this.userService = userService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns JWT access token and refresh token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "423", description = "Account locked")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "Customer registration",
            description = "Register a new customer account. Only customers can self-register."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or email already exists"),
            @ApiResponse(responseCode = "422", description = "Password validation failed")
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.registerCustomer(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/pharmacy-owner")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(
            summary = "Register pharmacy owner",
            description = "Create a new pharmacy owner account. Only Super Admin can perform this action.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pharmacy owner created"),
            @ApiResponse(responseCode = "403", description = "Access denied - Super Admin only"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<UserDto> registerPharmacyOwner(
            @Valid @RequestBody RegisterPharmacyOwnerRequest request) {

        Long adminId = securityUtils.getCurrentUserId().orElse(null);
        String adminEmail = securityUtils.getCurrentUserEmail().orElse("system");

        User user = authService.registerPharmacyOwner(request, adminId, adminEmail);
        return ResponseEntity.ok(mapToUserDto(user));
    }

    @PostMapping("/register/staff")
    @PreAuthorize("hasRole('PHARMACY_OWNER')")
    @Operation(
            summary = "Register staff member",
            description = "Create a new staff account for the pharmacy. Only Pharmacy Owner can perform this action.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff member created"),
            @ApiResponse(responseCode = "403", description = "Access denied - Pharmacy Owner only"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<UserDto> registerStaff(
            @Valid @RequestBody RegisterStaffRequest request) {

        Long ownerId = securityUtils.getCurrentUserId().orElse(null);
        String ownerEmail = securityUtils.getCurrentUserEmail().orElse("system");
        Long pharmacyId = securityUtils.getCurrentPharmacyId().orElse(null);

        User user = authService.registerStaff(request, ownerId, ownerEmail, pharmacyId);
        return ResponseEntity.ok(mapToUserDto(user));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Logout",
            description = "Invalidate the current access token",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Long userId = securityUtils.getCurrentUserId().orElse(null);
            String userEmail = securityUtils.getCurrentUserEmail().orElse("unknown");
            String ipAddress = SecurityUtils.getClientIP(request);

            authService.logout(token, userId, userEmail, ipAddress);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Get a new access token using a valid refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Get current user",
            description = "Get the profile of the currently authenticated user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned",
                    content = @Content(schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        Long userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not found"));
        User user = userService.getById(userId);
        return ResponseEntity.ok(mapToUserDto(user));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Change password",
            description = "Change the password for the current user",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Current password incorrect"),
            @ApiResponse(responseCode = "422", description = "New password validation failed")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        Long userId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new RuntimeException("User not found"));
        String ipAddress = SecurityUtils.getClientIP(httpRequest);

        authService.changePassword(request, userId, ipAddress);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/forgot-password")
    @Operation(
            summary = "Request password reset",
            description = "Send a password reset link to the user's email"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reset email sent (if account exists)")
    })
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIP(httpRequest);
        authService.requestPasswordReset(request.getEmail(), ipAddress);
        return ResponseEntity.ok(Map.of("message", "If an account exists, a password reset email has been sent"));
    }

    @GetMapping("/check-email")
    @Operation(
            summary = "Check email availability",
            description = "Check if an email address is available for registration"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed")
    })
    public ResponseEntity<Map<String, Boolean>> checkEmail(
            @Parameter(description = "Email to check") @RequestParam String email) {
        boolean available = !userService.emailExists(email);
        return ResponseEntity.ok(Map.of("available", available));
    }

    @GetMapping("/health")
    @Operation(
            summary = "Health check",
            description = "Check if the authentication service is running"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "OK", "service", "auth"));
    }

    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole().name());
        dto.setActive(user.isActive());
        dto.setEmailVerified(user.isEmailVerified());
        if (user.getPharmacy() != null) {
            dto.setPharmacyId(user.getPharmacy().getId());
            dto.setPharmacyName(user.getPharmacy().getName());
        }
        return dto;
    }
}