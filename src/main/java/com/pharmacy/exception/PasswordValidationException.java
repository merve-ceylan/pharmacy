package com.pharmacy.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class PasswordValidationException extends BaseException {

    private final List<String> errors;

    public PasswordValidationException(List<String> errors) {
        super("Password validation failed: " + String.join(", ", errors), HttpStatus.BAD_REQUEST, "PASSWORD_VALIDATION_FAILED");
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
