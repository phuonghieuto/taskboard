package com.phuonghieuto.backend.auth_service.service;

import com.phuonghieuto.backend.auth_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.auth_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserType;
import com.phuonghieuto.backend.auth_service.service.impl.TokenServiceImpl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private TokenConfigurationParameter tokenConfigurationParameter;

    @Mock
    private TokenValidationService tokenValidationService;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private PublicKey publicKey;
    private PrivateKey privateKey;
    private String validToken;
    private Date issuedAt;
    private Date expiresAt;

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
        validToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId("token-id-123").setSubject("test-subject")
                .setIssuedAt(issuedAt).setExpiration(expiresAt).claim(TokenClaims.USER_ID.getValue(), "user123")
                .claim(TokenClaims.USER_TYPE.getValue(), "ADMIN")
                .claim(TokenClaims.USER_EMAIL.getValue(), "test@example.com")
                .signWith(privateKey, SignatureAlgorithm.RS256).compact();
    }

    @Test
    void getAuthentication_Success() {
        // Arrange
        doReturn(false).when(tokenValidationService).verifyAndValidate(validToken);
        // Mock token configuration
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

        // Verify interactions with dependencies
        verify(tokenValidationService).verifyAndValidate(validToken);
        verify(tokenConfigurationParameter).getPublicKey();
    }

    @Test
    void getAuthentication_TokenValidationFails() {
        // Arrange
        doThrow(new RuntimeException("Token validation failed")).when(tokenValidationService)
                .verifyAndValidate(anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tokenService.getAuthentication("invalid-token"));

        assertEquals("Invalid token", exception.getMessage());
        verify(tokenValidationService).verifyAndValidate("invalid-token");
        verifyNoMoreInteractions(tokenConfigurationParameter);
    }

    @Test
    void getAuthentication_InvalidTokenFormat() {
        // Arrange
        doReturn(false).when(tokenValidationService).verifyAndValidate(anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tokenService.getAuthentication("malformed-token"));

        assertEquals("Invalid token", exception.getMessage());
        verify(tokenValidationService).verifyAndValidate("malformed-token");
        verify(tokenConfigurationParameter).getPublicKey();
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
        doReturn(true).when(tokenValidationService).verifyAndValidate(multiAuthToken);
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act
        UsernamePasswordAuthenticationToken auth = tokenService.getAuthentication(multiAuthToken);

        // Assert - note that the implementation only adds one authority based on
        // USER_TYPE
        assertNotNull(auth);
        assertEquals(1, auth.getAuthorities().size());

        // Find the authority and check its value
        String authority = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining());

        // The implementation should use the entire USER_TYPE value as one authority
        assertEquals("USER,MANAGER", authority);
    }

    @Test
    void getAuthentication_MissingUserTypeInToken() {
        
        // Create a token without user_type claim
        String noUserTypeToken = Jwts.builder().setHeaderParam("typ", "Bearer").setId("token-id-789")
                .setIssuedAt(issuedAt).setExpiration(expiresAt).claim(TokenClaims.USER_ID.getValue(), "user789")
                // Deliberately omit USER_TYPE claim
                .signWith(privateKey, SignatureAlgorithm.RS256).compact();

        // Arrange
        doReturn(false).when(tokenValidationService).verifyAndValidate(noUserTypeToken);
        when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Act
        UsernamePasswordAuthenticationToken auth = tokenService.getAuthentication(noUserTypeToken);

        // Assert - the implementation should handle null user_type
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());

        assertEquals(1, auth.getAuthorities().size());
        assertEquals(UserType.USER.name(), auth.getAuthorities().iterator().next().getAuthority().toString());
    }
}
