package com.goldmediatech.videometadata.service;

import com.goldmediatech.videometadata.config.AppConfig;
import com.goldmediatech.videometadata.dto.request.LoginRequest;
import com.goldmediatech.videometadata.dto.response.LoginResponse;
import com.goldmediatech.videometadata.security.JwtTokenProvider;
import com.goldmediatech.videometadata.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 * 
 * This test class demonstrates unit testing patterns for the authentication service
 * using Mockito for mocking dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AppConfig appConfig;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private UserPrincipal userPrincipal;
    private AppConfig.JwtConfig jwtConfig;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("admin", "admin123");
        
        userPrincipal = UserPrincipal.create(
                1L,
                "admin",
                "encodedPassword",
                List.of("ADMIN", "USER")
        );

        jwtConfig = new AppConfig.JwtConfig();
        jwtConfig.setSecret("testSecret");
        jwtConfig.setExpiration(86400000L);
    }

    @AfterEach
    void tearDown() {
        // Clear SecurityContext after each test
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticateUser_WithValidCredentials_ShouldReturnLoginResponse() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(tokenProvider.generateToken(authentication)).thenReturn("test.jwt.token");
        when(appConfig.getJwt()).thenReturn(jwtConfig);

        // When
        LoginResponse response = authService.authenticateUser(loginRequest);

        // Then
        assertNotNull(response);
        assertEquals("test.jwt.token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(86400000L, response.expiresIn());
        assertNotNull(response.userInfo());
        assertEquals("admin", response.userInfo().username());
        assertEquals(List.of("ADMIN", "USER"), response.userInfo().roles());
    }

    @Test
    void authenticateUser_WithInvalidCredentials_ShouldThrowException() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authService.authenticateUser(loginRequest);
        });
    }

    @Test
    void hasRole_WithAdminUser_ShouldReturnTrue() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(tokenProvider.generateToken(authentication)).thenReturn("test.jwt.token");
        when(appConfig.getJwt()).thenReturn(jwtConfig);

        // Set up security context with real context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Authenticate first to set the context
        authService.authenticateUser(loginRequest);

        // When & Then
        assertTrue(authService.hasRole("ADMIN"));
        assertTrue(authService.hasRole("USER"));
        assertFalse(authService.hasRole("INVALID_ROLE"));
    }

    @Test
    void isAdmin_WithAdminUser_ShouldReturnTrue() {
        // Given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(tokenProvider.generateToken(authentication)).thenReturn("test.jwt.token");
        when(appConfig.getJwt()).thenReturn(jwtConfig);

        // Set up security context with real context
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Authenticate first to set the context
        authService.authenticateUser(loginRequest);

        // When & Then
        assertTrue(authService.isAdmin());
    }

    @Test
    void hasRole_WithNoAuthentication_ShouldReturnFalse() {
        // Given - no authentication set up (SecurityContext is cleared in tearDown)
        
        // When & Then
        assertFalse(authService.hasRole("ADMIN"));
        assertFalse(authService.hasRole("USER"));
    }

    @Test
    void isAdmin_WithNoAuthentication_ShouldReturnFalse() {
        // Given - no authentication set up (SecurityContext is cleared in tearDown)
        
        // When & Then
        assertFalse(authService.isAdmin());
    }
} 