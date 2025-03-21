package com.phuonghieuto.backend.auth_service.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.auth_service.config.TestConfig;
import com.phuonghieuto.backend.auth_service.integration.BaseIntegrationTest;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
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

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController
 * Tests only the /users/* endpoints
 */
@Import(TestConfig.class)
@AutoConfigureMockMvc
public class UserControllerIntegrationTest extends BaseIntegrationTest {

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
        userRepository.flush();
    }
    
    @Test
    void register_Success() throws Exception {
        // Register a new user
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setEmail("test@example.com");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setPhoneNumber("1234567890");
        
        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
        
        // Verify user was created in database
        UserEntity user = userRepository.findByEmail("test@example.com").orElse(null);
        assertNotNull(user);
        assertTrue(passwordEncoder.matches("Password123!", user.getPassword()));
    }
    
    @Test
    void register_WithExistingEmail_Fails() throws Exception {
        // Create a user first
        UserEntity user = new UserEntity();
        user.setEmail("existing@example.com");
        user.setUsername("existinguser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Existing");
        user.setLastName("User");
        userRepository.save(user);
        
        // Try to register with the same email
        RegisterRequestDTO registerRequest = new RegisterRequestDTO();
        registerRequest.setEmail("existing@example.com");
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setPhoneNumber("1234567890");
        
        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value(containsString("The email is already used for another account: " + registerRequest.getEmail())));
    }
    
    @Test
    void getUserEmail_Success() throws Exception {
        // Create a user
        UserEntity user = new UserEntity();
        user.setEmail("email-test@example.com");
        user.setUsername("emailuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Email");
        user.setLastName("User");
        userRepository.save(user);
        
        String userId = user.getId();
        // Test getting user email by ID
        mockMvc.perform(get("/users/{userId}/email", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("email-test@example.com"));
    }
    
    @Test
    void getUserEmail_UserNotFound() throws Exception {
        String nonExistentId = UUID.randomUUID().toString();
        
        mockMvc.perform(get("/users/{userId}/email", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value(containsString("User not found")));
    }
    
    @Test
    void getUserIdFromEmail_Success() throws Exception {
        // Create a user
        UserEntity user = new UserEntity();
        user.setEmail("id-lookup@example.com");
        user.setUsername("idlookupuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("IdLookup");
        user.setLastName("User");
        userRepository.save(user);
        
        String userId = user.getId();
        // Test getting user ID by email
        mockMvc.perform(get("/users/by-email")
                .param("email", "id-lookup@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value("id-lookup@example.com"));
    }
    
    @Test
    void getUserIdFromEmail_UserNotFound() throws Exception {
        mockMvc.perform(get("/users/by-email")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value(containsString("User not found with email")));
    }
    
    @Test
    void confirmEmail_Success() throws Exception {
        // Create a user with unconfirmed email
        UserEntity user = new UserEntity();
        user.setEmail("confirm@example.com");
        user.setUsername("confirmuser");
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setFirstName("Confirm");
        user.setLastName("User");
        user.setEmailConfirmed(false);
        user.setConfirmationToken("valid-confirmation-token");
        userRepository.save(user);
        
        // Test confirming email with valid token
        mockMvc.perform(get("/users/confirm-email")
                .param("token", "valid-confirmation-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
        
        // Verify email is confirmed in database
        UserEntity confirmedUser = userRepository.findByEmail("confirm@example.com").orElse(null);
        assertNotNull(confirmedUser);
        assertTrue(confirmedUser.isEmailConfirmed());
    }
    
    @Test
    void confirmEmail_InvalidToken() throws Exception {
        mockMvc.perform(get("/users/confirm-email")
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid")));
    }
}