package com.pharmacy.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BaseException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(resource + " already exists with " + field + ": " + value, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }

    public DuplicateResourceException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
