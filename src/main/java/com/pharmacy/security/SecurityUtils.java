package com.pharmacy.security;

import com.pharmacy.entity.User;
import com.pharmacy.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Get current authentication
    public Optional<Authentication> getCurrentAuthentication() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of(auth);
        }
        return Optional.empty();
    }

    // Get current user's email
    public Optional<String> getCurrentUserEmail() {
        return getCurrentAuthentication()
                .map(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof UserDetails) {
                        return ((UserDetails) principal).getUsername();
                    }
                    return principal.toString();
                });
    }

    // Get current user entity
    public Optional<User> getCurrentUser() {
        return getCurrentUserEmail()
                .flatMap(userRepository::findByEmail);
    }

    // Get current user ID
    public Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }

    // Get current user's pharmacy ID
    public Optional<Long> getCurrentPharmacyId() {
        return getCurrentUser()
                .filter(user -> user.getPharmacy() != null)
                .map(user -> user.getPharmacy().getId());
    }

    // Check if current user has role
    public boolean hasRole(String role) {
        return getCurrentAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + role)))
                .orElse(false);
    }

    // Check if current user is Super Admin
    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    // Check if current user is Pharmacy Owner
    public boolean isPharmacyOwner() {
        return hasRole("PHARMACY_OWNER");
    }

    // Check if current user is Staff
    public boolean isStaff() {
        return hasRole("STAFF");
    }

    // Check if current user is Customer
    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    // Check if user can access pharmacy
    public boolean canAccessPharmacy(Long pharmacyId) {
        if (isSuperAdmin()) {
            return true;
        }
        return getCurrentPharmacyId()
                .map(id -> id.equals(pharmacyId))
                .orElse(false);
    }

    // Get client IP from request (static utility)
    public static String getClientIP(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
