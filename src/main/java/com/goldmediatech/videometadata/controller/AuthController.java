package com.goldmediatech.videometadata.controller;

import com.goldmediatech.videometadata.dto.request.LoginRequest;
import com.goldmediatech.videometadata.dto.response.LoginResponse;
import com.goldmediatech.videometadata.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication controller for handling login requests.
 * 
 * This controller provides endpoints for user authentication and JWT token generation.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    /**
     * Authenticate user and generate JWT token.
     * 
     * @param loginRequest the login request containing username and password
     * @return ResponseEntity with JWT token and user information
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate user with username and password, return JWT token. " +
                    "This endpoint does not require authentication. Use the returned token " +
                    "in the 'Authorize' button above to access other endpoints."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                schema = @Schema(implementation = LoginResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Login attempt for user: {}", loginRequest.username());
        
        try {
            LoginResponse response = authService.authenticateUser(loginRequest);
            logger.info("Login successful for user: {}", loginRequest.username());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Login failed for user: {} - {}", loginRequest.username(), e.getMessage());
            throw e;
        }
    }
} 