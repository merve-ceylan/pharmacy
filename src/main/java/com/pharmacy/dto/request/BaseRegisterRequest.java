package com.pharmacy.dto.request;

/**
 * Common interface for all registration requests
 * Enables shared validation logic
 */
public interface BaseRegisterRequest {
    String getEmail();
    String getPassword();
    String getFirstName();
    String getLastName();
    String getPhone();
}
