package com.pharmacy.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends BaseException {

    public AuthenticationException(String message) {
        super(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }

    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid email or password");
    }

    public static AuthenticationException accountLocked() {
        return new AuthenticationException("Account is locked. Please try again later");
    }

    public static AuthenticationException accountDisabled() {
        return new AuthenticationException("Account is disabled");
    }

    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("Token has expired");
    }

    public static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid token");
    }
}
