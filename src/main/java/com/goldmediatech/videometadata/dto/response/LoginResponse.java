package com.goldmediatech.videometadata.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for login authentication responses.
 * 
 * This class represents the response body for successful user authentication,
 * including the JWT token and user information.
 */
public record LoginResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Long expiresIn,

    @JsonProperty("user")
    UserInfo userInfo,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("issued_at")
    LocalDateTime issuedAt
) {
    public LoginResponse {
        if (tokenType == null) {
            tokenType = "Bearer";
        }
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }

    /**
     * Record representing user information in the login response.
     */
    public record UserInfo(
        String username,
        List<String> roles
    ) {}
} 