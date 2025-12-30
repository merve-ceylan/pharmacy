package com.pharmacy.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String errorCode;
    private String message;
    private List<String> details;
    private String path;

    // Private constructor for builder
    private ErrorResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public List<String> getDetails() { return details; }
    public String getPath() { return path; }

    // Builder class
    public static class Builder {
        private final ErrorResponse response = new ErrorResponse();

        public Builder timestamp(LocalDateTime timestamp) {
            response.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            response.status = status;
            return this;
        }

        public Builder error(String error) {
            response.error = error;
            return this;
        }

        public Builder errorCode(String errorCode) {
            response.errorCode = errorCode;
            return this;
        }

        public Builder message(String message) {
            response.message = message;
            return this;
        }

        public Builder details(List<String> details) {
            response.details = details;
            return this;
        }

        public Builder path(String path) {
            response.path = path;
            return this;
        }

        public ErrorResponse build() {
            return response;
        }
    }
}
