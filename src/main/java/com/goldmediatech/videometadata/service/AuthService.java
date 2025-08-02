package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.config.AppConfig;
import com.goldmediatech.videometadata.dto.request.LoginRequest;
import com.goldmediatech.videometadata.dto.response.LoginResponse;
import com.goldmediatech.videometadata.security.JwtTokenProvider;
import com.goldmediatech.videometadata.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication service for handling login operations and JWT token generation.
 * 
 * This service provides authentication functionality including user login
 * and JWT token generation for the Video Metadata Service.
 */
@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AppConfig appConfig;

    /**
     * Authenticate user and generate JWT token.
     * 
     * @param loginRequest the login request containing username and password
     * @return LoginResponse with JWT token and user information
     * @throws RuntimeException if authentication fails
     */
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.username(),
                            loginRequest.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = tokenProvider.generateToken(authentication);
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toList());

            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                    userPrincipal.getUsername(), roles);

            logger.info("User '{}' successfully authenticated", loginRequest.username());

            return new LoginResponse(jwt, "Bearer", appConfig.getJwt().getExpiration(), userInfo, LocalDateTime.now());

        } catch (Exception e) {
            logger.error("Authentication failed for user '{}': {}", 
                    loginRequest.username(), e.getMessage());
            throw new RuntimeException("Invalid username or password", e);
        }
    }

    /**
     * Get current authenticated user information.
     * 
     * @return UserPrincipal of the current authenticated user
     * @throws RuntimeException if no user is authenticated
     */
    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null) {
            throw new RuntimeException("No authenticated user found");
        }
        
        if (!authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        
        return (UserPrincipal) authentication.getPrincipal();
    }

    /**
     * Check if current user has a specific role.
     * 
     * @param role the role to check
     * @return true if user has the role, false otherwise
     */
    public boolean hasRole(String role) {
        try {
            UserPrincipal currentUser = getCurrentUser();
            return currentUser.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if current user is an admin.
     * 
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
} 