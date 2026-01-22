package com.pharmacy.service;

import com.pharmacy.entity.User;
import com.pharmacy.entity.Pharmacy;
import com.pharmacy.enums.UserRole;
import com.pharmacy.exception.ResourceNotFoundException;
import com.pharmacy.exception.DuplicateResourceException;
import com.pharmacy.exception.AuthenticationException;
import com.pharmacy.exception.PasswordValidationException;
import com.pharmacy.repository.UserRepository;
import com.pharmacy.security.PasswordValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       PasswordValidator passwordValidator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidator = passwordValidator;
    }

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new DuplicateResourceException("User", "email", user.getEmail());
        }

        // Validate password
        var result = passwordValidator.validate(user.getPassword());
        if (!result.valid()) {
            throw new PasswordValidationException(result.errors());
        }

        // Validate password against user info
        var userInfoResult = passwordValidator.validateAgainstUserInfo(
                user.getPassword(), user.getEmail(), user.getFirstName(), user.getLastName());
        if (!userInfoResult.valid()) {
            throw new PasswordValidationException(userInfoResult.errors());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        user.setEmailVerified(false);

        return userRepository.save(user);
    }

    public User createPharmacyOwner(User user, Pharmacy pharmacy) {
        user.setRole(UserRole.PHARMACY_OWNER);
        user.setPharmacy(pharmacy);
        return createUser(user);
    }

    public User createStaff(User user, Pharmacy pharmacy) {
        user.setRole(UserRole.STAFF);
        user.setPharmacy(pharmacy);
        return createUser(user);
    }

    public User createCustomer(User user) {
        user.setRole(UserRole.CUSTOMER);
        user.setPharmacy(null);
        return createUser(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public Optional<User> findByEmailAndPharmacy(String email, Long pharmacyId) {
        return userRepository.findByEmailAndPharmacyId(email, pharmacyId);
    }

    public List<User> findByPharmacy(Long pharmacyId) {
        return userRepository.findByPharmacyId(pharmacyId);
    }

    public List<User> findStaffByPharmacy(Long pharmacyId) {
        return userRepository.findByPharmacyIdAndRole(pharmacyId, UserRole.STAFF);
    }

    public List<User> findAllCustomers() {
        return userRepository.findByRole(UserRole.CUSTOMER);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public User updatePassword(Long userId, String newPassword) {
        User user = getById(userId);

        var result = passwordValidator.validate(newPassword);
        if (!result.valid()) {
            throw new PasswordValidationException(result.errors());
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public void updateLastLogin(Long userId) {
        User user = getById(userId);
        user.setLastLogin(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
    }

    public void handleFailedLogin(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }

            userRepository.save(user);
        }
    }

    public void validateUserCanLogin(User user) {
        if (!user.isActive()) {
            throw AuthenticationException.accountDisabled();
        }
        if (user.isLocked()) {
            throw AuthenticationException.accountLocked();
        }
    }

    public User deactivateUser(Long userId) {
        User user = getById(userId);
        user.setActive(false);
        return userRepository.save(user);
    }

    public User activateUser(Long userId) {
        User user = getById(userId);
        user.setActive(true);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        return userRepository.save(user);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> findAll() {
        return userRepository.findAllWithPharmacy();
    }

    public List<User> findByRole(UserRole role) {
        return userRepository.findByRoleWithPharmacy(role);
    }
}
