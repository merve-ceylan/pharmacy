package com.pharmacy.service;

import com.pharmacy.dto.common.UserDto;
import com.pharmacy.dto.request.*;
import com.pharmacy.dto.response.AuthResponse;
import com.pharmacy.entity.User;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.AuthenticationException;
import com.pharmacy.exception.BadRequestException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.exception.PasswordValidationException;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.repository.UserRepository;
import com.pharmacy.repository.PharmacyRepository;
import com.pharmacy.security.JwtService;
import com.pharmacy.security.PasswordValidator;
import com.pharmacy.security.TokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PharmacyRepository pharmacyRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       PharmacyRepository pharmacyRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordValidator passwordValidator,
                       JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.auditLogService = auditLogService;
    }

    /**
     * Authenticate user and generate JWT token
     */
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for email: {} from IP: {}", request.getEmail(), ipAddress);

        // Validate request
        validateLoginRequest(request);

        // Find user
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseGet(() -> {
                    // Log failed attempt even if user doesn't exist (prevent user enumeration)
                    auditLogService.logUserLoginFailed(request.getEmail(), ipAddress, "User not found");
                    log.warn("Login failed - user not found: {}", request.getEmail());
                    throw AuthenticationException.invalidCredentials();
                });

        // Check if account is locked
        if (user.isLocked()) {
            auditLogService.logUserLoginFailed(request.getEmail(), ipAddress, "Account locked");
            log.warn("Login failed - account locked: {}", request.getEmail());
            throw AuthenticationException.accountLocked();
        }

        // Check if account is active
        if (!user.isActive()) {
            auditLogService.logUserLoginFailed(request.getEmail(), ipAddress, "Account disabled");
            log.warn("Login failed - account disabled: {}", request.getEmail());
            throw AuthenticationException.accountDisabled();
        }

        // Check pharmacy status for pharmacy users
        if (user.getPharmacy() != null) {
            Pharmacy pharmacy = user.getPharmacy();
            if (!pharmacy.isActive()) {
                auditLogService.logUserLoginFailed(request.getEmail(), ipAddress, "Pharmacy suspended");
                log.warn("Login failed - pharmacy suspended: {}", request.getEmail());
                throw new AuthenticationException("Pharmacy account is suspended. Please contact support.");
            }
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user, ipAddress);
            throw AuthenticationException.invalidCredentials();
        }

        // Success - reset failed attempts and update last login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate tokens
        Map<String, Object> claims = buildUserClaims(user);
        String accessToken = jwtService.generateToken(claims, buildUserDetails(user));
        String refreshToken = jwtService.generateRefreshToken(buildUserDetails(user));

        // Audit log
        auditLogService.logUserLogin(user.getId(), user.getEmail(), ipAddress, userAgent);
        log.info("Login successful for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime() / 1000)
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Register new customer
     */
    public AuthResponse registerCustomer(RegisterRequest request, String ipAddress, String userAgent) {
        log.info("Customer registration attempt for email: {} from IP: {}", request.getEmail(), ipAddress);

        // Validate request
        validateRegisterRequest(request);

        // Check if email already exists
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed - email exists: {}", email);
            throw new DuplicateResourceException("User", "email", email);
        }

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(sanitizeInput(request.getFirstName()));
        user.setLastName(sanitizeInput(request.getLastName()));
        user.setPhone(sanitizePhone(request.getPhone()));
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);

        user = userRepository.save(user);

        // Generate tokens
        Map<String, Object> claims = buildUserClaims(user);
        String accessToken = jwtService.generateToken(claims, buildUserDetails(user));
        String refreshToken = jwtService.generateRefreshToken(buildUserDetails(user));

        // Audit log
        auditLogService.logUserCreated(null, user.getId(), user.getEmail(),
                user.getId(), user.getEmail(), UserRole.CUSTOMER.name());
        log.info("Customer registration successful: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime() / 1000)
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Register pharmacy owner (Super Admin only)
     */
    public User registerPharmacyOwner(RegisterPharmacyOwnerRequest request, Long adminId, String adminEmail) {
        log.info("Pharmacy owner registration by admin: {} for email: {}", adminEmail, request.getEmail());

        // Validate request
        validateRegisterRequest(request);

        // Check if email already exists
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        // Get pharmacy
        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", request.getPharmacyId()));

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(sanitizeInput(request.getFirstName()));
        user.setLastName(sanitizeInput(request.getLastName()));
        user.setPhone(sanitizePhone(request.getPhone()));
        user.setRole(UserRole.PHARMACY_OWNER);
        user.setPharmacy(pharmacy);
        user.setActive(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);

        user = userRepository.save(user);

        // Audit log
        auditLogService.logUserCreated(pharmacy.getId(), adminId, adminEmail,
                user.getId(), user.getEmail(), UserRole.PHARMACY_OWNER.name());
        log.info("Pharmacy owner registration successful: {} for pharmacy: {}", user.getEmail(), pharmacy.getName());

        return user;
    }

    /**
     * Register staff (Pharmacy Owner only)
     */
    public User registerStaff(RegisterStaffRequest request, Long ownerId, String ownerEmail, Long pharmacyId) {
        log.info("Staff registration by owner: {} for email: {}", ownerEmail, request.getEmail());

        // Validate request
        validateRegisterRequest(request);

        // Check if email already exists in this pharmacy
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmailAndPharmacyId(email, pharmacyId)) {
            throw new DuplicateResourceException("Staff", "email", email);
        }

        // Get pharmacy
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("Pharmacy", pharmacyId));

        // Create user
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(sanitizeInput(request.getFirstName()));
        user.setLastName(sanitizeInput(request.getLastName()));
        user.setPhone(sanitizePhone(request.getPhone()));
        user.setRole(UserRole.STAFF);
        user.setPharmacy(pharmacy);
        user.setActive(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setFailedLoginAttempts(0);

        user = userRepository.save(user);

        // Audit log
        auditLogService.logUserCreated(pharmacyId, ownerId, ownerEmail,
                user.getId(), user.getEmail(), UserRole.STAFF.name());
        log.info("Staff registration successful: {} for pharmacy: {}", user.getEmail(), pharmacy.getName());

        return user;
    }

    /**
     * Logout - blacklist token
     */
    public void logout(String token, Long userId, String userEmail, String ipAddress) {
        log.info("Logout for user: {}", userEmail);

        try {
            // Extract expiration from token and blacklist
            Date expiration = new Date(System.currentTimeMillis() + jwtService.getExpirationTime());
            tokenBlacklistService.blacklistToken(token, expiration);

            // Audit log
            auditLogService.logUserLogout(userId, userEmail, ipAddress);
            log.info("Logout successful for user: {}", userEmail);
        } catch (Exception e) {
            log.error("Error during logout for user: {}", userEmail, e);
            // Don't throw - logout should always succeed from user's perspective
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Token refresh attempt");

        // Check if token is blacklisted
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw AuthenticationException.invalidToken();
        }

        // Extract username and validate
        String email;
        try {
            email = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            throw AuthenticationException.invalidToken();
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AuthenticationException.invalidToken());

        // Validate user is still active
        if (!user.isActive()) {
            throw AuthenticationException.accountDisabled();
        }

        if (user.isLocked()) {
            throw AuthenticationException.accountLocked();
        }

        // Generate new access token
        Map<String, Object> claims = buildUserClaims(user);
        String newAccessToken = jwtService.generateToken(claims, buildUserDetails(user));

        log.debug("Token refresh successful for user: {}", email);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationTime() / 1000)
                .user(mapToUserDto(user))
                .build();
    }

    /**
     * Change password (authenticated user)
     */
    public void changePassword(ChangePasswordRequest request, Long userId, String ipAddress) {
        log.info("Password change attempt for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            auditLogService.log(user.getPharmacy() != null ? user.getPharmacy().getId() : null,
                    userId, user.getEmail(), "PASSWORD_CHANGE_FAILED", "USER", userId,
                    null, null, "Invalid current password");
            throw new BadRequestException("Current password is incorrect");
        }

        // Validate new password
        var result = passwordValidator.validate(request.getNewPassword());
        if (!result.valid()) {
            throw new PasswordValidationException(result.errors());
        }

        // Check password is different
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        // Validate against user info
        var userInfoResult = passwordValidator.validateAgainstUserInfo(
                request.getNewPassword(), user.getEmail(), user.getFirstName(), user.getLastName());
        if (!userInfoResult.valid()) {
            throw new PasswordValidationException(userInfoResult.errors());
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Audit log
        auditLogService.logPasswordChanged(userId, user.getEmail(), ipAddress);
        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    /**
     * Request password reset - generates reset token
     */
    public void requestPasswordReset(String email, String ipAddress) {
        log.info("Password reset request for email: {} from IP: {}", email, ipAddress);

        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());

        // Always return success to prevent user enumeration
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Generate reset token (would normally send via email)
        String resetToken = UUID.randomUUID().toString();

        // TODO: Store reset token with expiration and send email
        // For now, just log it
        log.info("Password reset token generated for user: {} - Token: {}", email, resetToken);

        auditLogService.log(user.getPharmacy() != null ? user.getPharmacy().getId() : null,
                user.getId(), user.getEmail(), "PASSWORD_RESET_REQUESTED", "USER", user.getId(),
                null, null, "Password reset requested from IP: " + ipAddress);
    }

    /**
     * Get current user info
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return mapToUserDto(user);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private void handleFailedLogin(User user, String ipAddress) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        String reason = "Invalid password (attempt " + attempts + " of " + MAX_FAILED_ATTEMPTS + ")";

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            reason = "Account locked after " + attempts + " failed attempts";
            auditLogService.logUserAccountLocked(user.getId(), user.getEmail(), attempts);
            log.warn("Account locked due to failed attempts: {}", user.getEmail());
        }

        userRepository.save(user);
        auditLogService.logUserLoginFailed(user.getEmail(), ipAddress, reason);
        log.warn("Login failed for user: {} - {}", user.getEmail(), reason);
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new BadRequestException("Login request cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Password is required");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new BadRequestException("Invalid email format");
        }
    }

    private void validateRegisterRequest(BaseRegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration request cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new BadRequestException("Invalid email format");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new BadRequestException("Password is required");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new BadRequestException("First name is required");
        }
        if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new BadRequestException("Last name is required");
        }

        // Validate password strength
        var result = passwordValidator.validate(request.getPassword());
        if (!result.valid()) {
            throw new PasswordValidationException(result.errors());
        }

        // Validate password against user info
        var userInfoResult = passwordValidator.validateAgainstUserInfo(
                request.getPassword(), request.getEmail(), request.getFirstName(), request.getLastName());
        if (!userInfoResult.valid()) {
            throw new PasswordValidationException(userInfoResult.errors());
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return input.trim()
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;");
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9+]", "");
    }

    private Map<String, Object> buildUserClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        if (user.getPharmacy() != null) {
            claims.put("pharmacyId", user.getPharmacy().getId());
            claims.put("pharmacyName", user.getPharmacy().getName());
        }
        return claims;
    }

    private org.springframework.security.core.userdetails.User buildUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                java.util.Collections.singletonList(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
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
        dto.setLastLogin(user.getLastLogin());
        if (user.getPharmacy() != null) {
            dto.setPharmacyId(user.getPharmacy().getId());
            dto.setPharmacyName(user.getPharmacy().getName());
        }
        return dto;
    }
}
