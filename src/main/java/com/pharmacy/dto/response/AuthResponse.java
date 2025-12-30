package com.pharmacy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pharmacy.dto.common.UserDto;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserDto user;

    // Private constructor for builder
    private AuthResponse() {}

    public static Builder builder() {
        return new Builder();
    }

    // Getters
    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public Long getExpiresIn() { return expiresIn; }
    public UserDto getUser() { return user; }

    // Builder
    public static class Builder {
        private final AuthResponse response = new AuthResponse();

        public Builder accessToken(String accessToken) {
            response.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            response.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            response.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            response.expiresIn = expiresIn;
            return this;
        }

        public Builder user(UserDto user) {
            response.user = user;
            return this;
        }

        public AuthResponse build() {
            return response;
        }
    }
}
