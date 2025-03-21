package com.phuonghieuto.backend.auth_service.unit.service;

import com.phuonghieuto.backend.auth_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.auth_service.exception.TokenAlreadyInvalidatedException;
import com.phuonghieuto.backend.auth_service.service.TokenGenerationService;
import com.phuonghieuto.backend.auth_service.service.TokenManagementService;
import com.phuonghieuto.backend.auth_service.service.impl.TokenValidationServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenValidationServiceImplTest {

    @Mock
    private TokenConfigurationParameter tokenConfigurationParameter;

    @Mock
    private TokenManagementService tokenManagementService;

    @InjectMocks
    private TokenValidationServiceImpl tokenValidationService;

    @Mock
    private TokenGenerationService tokenGenerationService;

    @Mock
    private Jws<Claims> mockJws;

    @Mock
    private Claims mockClaims;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String validToken;
    private String validTokenId = "test-token-id";

    @BeforeEach
    void setUp() throws Exception {
        // Generate a key pair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey = keyPair.getPrivate();

        // Configure tokenConfigurationParameter to use our test keys

        // Generate a real JWT token using the actual builder
        validToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId(validTokenId).setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10000)).signWith(privateKey)
                .claim("userId", "test-user").compact();
    }

    @Test
    void verifyAndValidate_String_Success() {
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);
        doReturn(false).when(tokenManagementService).checkForInvalidityOfToken(validTokenId);

        // Execute the method
        boolean result = tokenValidationService.verifyAndValidate(validToken);

        // Verify results
        assertTrue(result);
        verify(tokenManagementService).checkForInvalidityOfToken(validTokenId);
    }

    @Test
    void verifyAndValidate_String_TokenAlreadyInvalidated() {
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Mock token management service to throw exception
        doThrow(new TokenAlreadyInvalidatedException("Token is already invalidated")).when(tokenManagementService)
                .checkForInvalidityOfToken(validTokenId);

        // Execute and verify
        assertThrows(TokenAlreadyInvalidatedException.class, () -> tokenValidationService.verifyAndValidate(validToken));
        verify(tokenManagementService).checkForInvalidityOfToken(validTokenId);
    }

    @Test
    void verifyAndValidate_String_ExpiredToken() {
        // Generate an expired token
        String expiredToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId("expired-token-id")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20000)) // 20 seconds in past
                .setExpiration(new Date(System.currentTimeMillis() - 10000)) // 10 seconds in past
                .signWith(privateKey).claim("userId", "test-user").compact();

        // Execute and verify
        assertThrows(ResponseStatusException.class, () -> tokenValidationService.verifyAndValidate(expiredToken));
    }

    @Test
    void verifyAndValidate_String_InvalidToken() {
        // Use a malformed token
        String invalidToken = "invalid.jwt.token";

        // Execute and verify
        assertThrows(ResponseStatusException.class, () -> tokenValidationService.verifyAndValidate(invalidToken));
    }

    @Test
    void verifyAndValidate_String_GenericException() {
        TokenValidationServiceImpl spyService = spy(tokenValidationService);

        doAnswer(invocation -> {
            tokenManagementService.checkForInvalidityOfToken(validTokenId);

            throw new RuntimeException("Generic error");
        }).when(spyService).verifyAndValidate(validToken);

        // Execute and verify
        assertThrows(RuntimeException.class, () -> spyService.verifyAndValidate(validToken));

        // Verify that tokenManagementService was called
        verify(tokenManagementService).checkForInvalidityOfToken(validTokenId);
    }

    @Test
    void verifyAndValidate_Set_AllValid() {
        // Create a set of tokens
        Set<String> tokens = Set.of("token1", "token2", "token3");

        // Create a spy to test the method without actually calling JWT library
        TokenValidationServiceImpl spyService = spy(tokenValidationService);

        // Mock the single token validation method to return true
        doReturn(true).when(spyService).verifyAndValidate(anyString());

        // Execute the method
        boolean result = spyService.verifyAndValidate(tokens);

        // Verify results
        assertTrue(result);
        verify(spyService, times(3)).verifyAndValidate(anyString());
    }

    @Test
    void verifyAndValidate_Set_OneInvalid() {
        // Create a set of tokens with a specific order
        Set<String> tokens = new HashSet<>();
        tokens.add("valid-token1");
        tokens.add("invalid-token");
        tokens.add("valid-token2");

        // Create a spy to test the method without actually calling JWT library
        TokenValidationServiceImpl spyService = spy(tokenValidationService);

        // Mock behavior for specific tokens
        doReturn(false).when(spyService).verifyAndValidate("invalid-token");

        // Execute the method
        boolean result = spyService.verifyAndValidate(tokens);

        // Verify results
        assertFalse(result);
        verify(spyService, atMost(3)).verifyAndValidate(anyString());
    }
}