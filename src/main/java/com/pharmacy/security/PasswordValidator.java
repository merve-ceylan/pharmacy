package com.pharmacy.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    // Patterns
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    private static final Pattern HAS_WHITESPACE = Pattern.compile("\\s");

    // Common weak passwords to reject
    private static final List<String> WEAK_PASSWORDS = List.of(
            "password", "123456", "12345678", "qwerty", "abc123",
            "password123", "admin", "letmein", "welcome", "monkey",
            "dragon", "master", "login", "princess", "solo"
    );

    public ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required");
            return new ValidationResult(false, errors);
        }

        // Length check
        if (password.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters");
        }

        if (password.length() > MAX_LENGTH) {
            errors.add("Password must not exceed " + MAX_LENGTH + " characters");
        }

        // Complexity checks
        if (!HAS_UPPERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        if (!HAS_LOWERCASE.matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        if (!HAS_DIGIT.matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }

        if (!HAS_SPECIAL.matcher(password).find()) {
            errors.add("Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)");
        }

        // No whitespace
        if (HAS_WHITESPACE.matcher(password).find()) {
            errors.add("Password must not contain whitespace");
        }

        // Check against weak passwords
        if (WEAK_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common. Please choose a stronger password");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // Validate password doesn't contain user info
    public ValidationResult validateAgainstUserInfo(String password, String email, String firstName, String lastName) {
        List<String> errors = new ArrayList<>();

        String lowerPassword = password.toLowerCase();

        if (email != null && lowerPassword.contains(email.split("@")[0].toLowerCase())) {
            errors.add("Password must not contain your email");
        }

        if (firstName != null && firstName.length() >= 3 && lowerPassword.contains(firstName.toLowerCase())) {
            errors.add("Password must not contain your first name");
        }

        if (lastName != null && lastName.length() >= 3 && lowerPassword.contains(lastName.toLowerCase())) {
            errors.add("Password must not contain your last name");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    // Calculate password strength (0-100)
    public int calculateStrength(String password) {
        if (password == null || password.isEmpty()) return 0;

        int score = 0;

        // Length score (up to 30 points)
        score += Math.min(password.length() * 2, 30);

        // Complexity score (up to 40 points)
        if (HAS_UPPERCASE.matcher(password).find()) score += 10;
        if (HAS_LOWERCASE.matcher(password).find()) score += 10;
        if (HAS_DIGIT.matcher(password).find()) score += 10;
        if (HAS_SPECIAL.matcher(password).find()) score += 10;

        // Variety bonus (up to 30 points)
        long uniqueChars = password.chars().distinct().count();
        score += Math.min((int)(uniqueChars * 2), 30);

        return Math.min(score, 100);
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}
