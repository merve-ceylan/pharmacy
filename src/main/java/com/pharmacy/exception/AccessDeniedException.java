package com.pharmacy.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends BaseException {

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }

    public static AccessDeniedException noPermission() {
        return new AccessDeniedException("You don't have permission to perform this action");
    }

    public static AccessDeniedException pharmacyAccess(Long pharmacyId) {
        return new AccessDeniedException("You don't have access to pharmacy: " + pharmacyId);
    }

    public static AccessDeniedException resourceAccess(String resource) {
        return new AccessDeniedException("You don't have access to this " + resource);
    }
}
