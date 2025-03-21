package com.phuonghieuto.backend.auth_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.auth_service.config.TestConfig;
import com.phuonghieuto.backend.auth_service.integration.BaseIntegrationTest;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AuthController
 * Tests only the /auth/* endpoints
 */
@Import(TestConfig.class)
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Clear the database before each test
        userRepository.deleteAll();
    }
    
    @Test
    void login_Success() throws Exception {
        // Create user in the database
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmailConfirmed(true);
        userRepository.save(user);
        
        // Login with valid credentials
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("Password123!");
        
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.accessToken").exists())
                .andExpect(jsonPath("$.response.refreshToken").exists());
    }
    
    @Test
    void login_InvalidCredentials() throws Exception {
        // Create user in the database
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmailConfirmed(true);
        userRepository.save(user);
        
        // Try to login with wrong password
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("WrongPassword123!");
        
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value("Password is not valid"));
    }
    
    @Test
    void refreshToken_Success() throws Exception {
        // Create and login a user to get tokens
        UserEntity user = new UserEntity();
        user.setEmail("refresh@example.com");
        user.setUsername("refreshuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Refresh");
        user.setLastName("User");
        user.setEmailConfirmed(true);
        userRepository.save(user);
        
        // Login
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("refresh@example.com");
        loginRequest.setPassword("Password123!");
        
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract refresh token from response
        String responseJson = loginResult.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseJson)
                .path("response")
                .path("refreshToken")
                .asText();
        
        // Test refresh token endpoint
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken(refreshToken);
        
        mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.accessToken").exists())
                .andExpect(jsonPath("$.response.refreshToken").exists());
    }
    
    @Test
    void refreshToken_WithInvalidToken_Fails() throws Exception {
        // Try to refresh with an invalid token
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("invalid.refresh.token");
        
        mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid JWT token")));
    }
    
    @Test
    void logout_Success() throws Exception {
        // Create and login a user to get tokens
        UserEntity user = new UserEntity();
        user.setEmail("logout@example.com");
        user.setUsername("logoutuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Logout");
        user.setLastName("User");
        user.setEmailConfirmed(true);
        userRepository.save(user);
        
        // Login
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("logout@example.com");
        loginRequest.setPassword("Password123!");
        
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract tokens from response
        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson)
                .path("response")
                .path("accessToken")
                .asText();
        String refreshToken = objectMapper.readTree(responseJson)
                .path("response")
                .path("refreshToken")
                .asText();
        
        // Test logout endpoint
        TokenInvalidateRequestDTO logoutRequest = new TokenInvalidateRequestDTO();
        logoutRequest.setAccessToken(accessToken);
        logoutRequest.setRefreshToken(refreshToken);
        
        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
        
        // Try to refresh token after logout (should fail)
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken(refreshToken);
        
        mockMvc.perform(post("/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void validateToken_Success() throws Exception {
        // Create and login a user to get tokens
        UserEntity user = new UserEntity();
        user.setEmail("validate@example.com");
        user.setUsername("validateuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Validate");
        user.setLastName("User");
        user.setEmailConfirmed(true);
        userRepository.save(user);
        
        // Login
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("validate@example.com");
        loginRequest.setPassword("Password123!");
        
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract access token from response
        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson)
                .path("response")
                .path("accessToken")
                .asText();
        
        // Test validate token endpoint
        mockMvc.perform(post("/auth/validate-token")
                .param("token", accessToken))
                .andExpect(status().isOk());
    }
    
    @Test
    void validateToken_WithInvalidToken_Fails() throws Exception {
        mockMvc.perform(post("/auth/validate-token")
                .param("token", "invalid.access.token"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getAuthentication_Success() throws Exception {
        // Create and login a user to get tokens
        UserEntity user = new UserEntity();
        user.setEmail("auth@example.com");
        user.setUsername("authuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Auth");
        user.setLastName("User");
        user.setEmailConfirmed(true);
        userRepository.save(user);
        
        // Login
        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("auth@example.com");
        loginRequest.setPassword("Password123!");
        
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Extract access token from response
        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson)
                .path("response")
                .path("accessToken")
                .asText();
        
        // Test get authentication endpoint
        mockMvc.perform(get("/auth/authenticate")
                .param("token", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.principal").exists())
                .andExpect(jsonPath("$.authorities").isArray());
    }
    
    @Test
    void getAuthentication_WithInvalidToken_Fails() throws Exception {
        mockMvc.perform(get("/auth/authenticate")
                .param("token", "invalid.access.token"))
                .andExpect(status().isUnauthorized());
    }
}