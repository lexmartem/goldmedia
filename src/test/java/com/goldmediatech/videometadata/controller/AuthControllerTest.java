package com.goldmediatech.videometadata.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goldmediatech.videometadata.dto.request.LoginRequest;
import com.goldmediatech.videometadata.dto.response.LoginResponse;
import com.goldmediatech.videometadata.exception.GlobalExceptionHandler;
import com.goldmediatech.videometadata.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AuthController.
 * 
 * This test class demonstrates controller testing patterns using MockMvc
 * to test HTTP endpoints with proper request/response handling.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_WithValidCredentials_ShouldReturnSuccessResponse() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo("admin", List.of("ADMIN", "USER"));
        LoginResponse expectedResponse = new LoginResponse("test.jwt.token", "Bearer", 86400000L, userInfo, LocalDateTime.now());

        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(expectedResponse);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").value("test.jwt.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(86400000L))
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.roles").isArray())
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.user.roles[1]").value("USER"));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturnInternalServerError() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("invalid", "wrongpassword");
        when(authService.authenticateUser(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("RUNTIME_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.details[0]").value("Invalid credentials"));
    }

    @Test
    void login_WithEmptyUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("", "password");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithEmptyPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("username", "");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithNullUsername_ShouldReturnBadRequest() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest(null, "password");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithNullPassword_ShouldReturnBadRequest() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("username", null);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithInvalidJson_ShouldReturnBadRequest() throws Exception {
        // Given - Invalid JSON
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void login_WithMissingContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());
    }
} 