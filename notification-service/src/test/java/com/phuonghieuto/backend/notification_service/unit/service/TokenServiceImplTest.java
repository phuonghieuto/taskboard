package com.phuonghieuto.backend.notification_service.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import com.phuonghieuto.backend.notification_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.notification_service.model.auth.UserType;
import com.phuonghieuto.backend.notification_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.notification_service.service.impl.TokenServiceImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@ExtendWith(MockitoExtension.class)
public class TokenServiceImplTest {
    @Mock
    private TokenConfigurationParameter tokenConfigurationParameter;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String validToken;
    private String validTokenId = "test-token-id";
    private Date issuedAt;
    private Date expiresAt;

    @Mock
    private Jws<Claims> mockJws;

    @Mock
    private Claims mockClaims;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a key pair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        // Set up dates for token creation
        issuedAt = new Date();
        expiresAt = new Date(issuedAt.getTime() + 3600000); // 1 hour later

        // Create a valid JWT token
        validToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId(validTokenId).setSubject("test-subject")
                .setIssuedAt(issuedAt).setExpiration(expiresAt).claim(TokenClaims.USER_ID.getValue(), "user123")
                .claim(TokenClaims.USER_TYPE.getValue(), "ADMIN")
                .claim(TokenClaims.USER_EMAIL.getValue(), "test@example.com")
                .signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    @Test
    void validateToken_Success() {
        // Arrange
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act & Assert - no exception should be thrown
        assertDoesNotThrow(() -> tokenService.validateToken(validToken));

        // Verify that the configuration was accessed
        verify(tokenConfigurationParameter).getPublicKey();
    }

    @Test
    void validateToken_ExpiredToken() {
        // Arrange
        // Create an expired token
        String expiredToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId("expired-token-id")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20000)) // 20 seconds ago
                .setExpiration(new Date(System.currentTimeMillis() - 10000)) // 10 seconds ago
                .claim(TokenClaims.USER_ID.getValue(), "user123").signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tokenService.validateToken(expiredToken));

        // Verify exception details
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Token has expired"));
        verify(tokenConfigurationParameter).getPublicKey();
    }

    @Test
    void validateToken_InvalidTokenFormat() {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tokenService.validateToken(invalidToken));

        // Verify exception details
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
        verify(tokenConfigurationParameter).getPublicKey();
    }

    @Test
    void validateToken_TamperedToken() {
        // Arrange
        // Create a valid token and then tamper with it
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalid_signature";

        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tokenService.validateToken(tamperedToken));

        // Verify exception details
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
        verify(tokenConfigurationParameter).getPublicKey();
    }

    @Test
    void validateToken_NullToken() {
        // Arrange
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tokenService.validateToken(null));

        // Verify exception details
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Error validating token"));
    }

    @Test
    void validateToken_PublicKeyNotAvailable() {
        // Arrange
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(null);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tokenService.validateToken(validToken));

        // Verify exception details
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Error validating token"));
        verify(tokenConfigurationParameter).getPublicKey();
    }

    @Test
    void getAuthentication_Success() {
        // Arrange
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act
        UsernamePasswordAuthenticationToken auth = tokenService.getAuthentication(validToken);

        // Assert
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());

        // Verify the principal is a Jwt object
        assertTrue(auth.getPrincipal() instanceof Jwt);
        Jwt jwt = (Jwt) auth.getPrincipal();

        // Verify JWT contents
        assertEquals("user123", jwt.getClaim(TokenClaims.USER_ID.getValue()));
        assertEquals("ADMIN", jwt.getClaim(TokenClaims.USER_TYPE.getValue()));
        assertEquals("test@example.com", jwt.getClaim(TokenClaims.USER_EMAIL.getValue()));

        // Check issued at and expiration times
        assertEquals(issuedAt.toInstant().truncatedTo(ChronoUnit.SECONDS), jwt.getIssuedAt());
        assertEquals(expiresAt.toInstant().truncatedTo(ChronoUnit.SECONDS), jwt.getExpiresAt());

        // Verify JWT headers
        assertEquals("Bearer", jwt.getHeaders().get(TokenClaims.TYP.getValue()));
        assertEquals("RS256", jwt.getHeaders().get(TokenClaims.ALGORITHM.getValue()));

        // Verify the authorities
        assertNotNull(auth.getAuthorities());
        assertEquals(1, auth.getAuthorities().size());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN")));

        // Verify the credentials are null (as per the implementation)
        assertNull(auth.getCredentials());

        verify(tokenConfigurationParameter, times(2)).getPublicKey();
    }

    @Test
    void getAuthentication_NullToken() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> tokenService.getAuthentication(null));

        assertEquals("Invalid token", exception.getMessage());
    }

    @Test
    void getAuthentication_WithMultipleAuthorities() {
        // Create a token with multiple authority types
        String multiAuthToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId("token-id-456")
                .setIssuedAt(issuedAt).setExpiration(expiresAt).claim(TokenClaims.USER_ID.getValue(), "user456")
                .claim(TokenClaims.USER_TYPE.getValue(), "USER,MANAGER").signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        // Arrange
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act
        UsernamePasswordAuthenticationToken auth = tokenService.getAuthentication(multiAuthToken);

        // Assert
        assertNotNull(auth);
        assertEquals(1, auth.getAuthorities().size());

        // Find the authority and check its value
        String authority = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());

        // The implementation should use the entire USER_TYPE value as one authority
        assertEquals("USER,MANAGER", authority);

        verify(tokenConfigurationParameter, times(2)).getPublicKey();
    }

    @Test
    void getAuthentication_MissingUserTypeInToken() {
        // Create a token without user_type claim
        String noUserTypeToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId("token-id-789")
                .setIssuedAt(issuedAt).setExpiration(expiresAt).claim(TokenClaims.USER_ID.getValue(), "user789")
                // Deliberately omit USER_TYPE claim
                .signWith(privateKey, SignatureAlgorithm.RS256).compact();

        // Arrange
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act
        UsernamePasswordAuthenticationToken auth = tokenService.getAuthentication(noUserTypeToken);

        // Assert - should handle null user_type gracefully
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals(1, auth.getAuthorities().size());

        assertEquals(UserType.USER.name(), auth.getAuthorities().iterator().next().getAuthority().toString());
        verify(tokenConfigurationParameter, times(2)).getPublicKey();
    }
}