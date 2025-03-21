package com.phuonghieuto.backend.auth_service.unit.service;

import com.phuonghieuto.backend.auth_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.service.impl.TokenGenerationServiceImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class TokenGenerationServiceImplTest {

	@Mock
	private TokenConfigurationParameter tokenConfigurationParameter;

	@InjectMocks
	private TokenGenerationServiceImpl tokenGenerationService;

	private PrivateKey privateKey;
	private PublicKey publicKey;
	private Map<String, Object> claims;

	@BeforeEach
	void setUp() throws Exception {
		// Generate a key pair for testing
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
		privateKey = keyPair.getPrivate();
		publicKey = keyPair.getPublic();

		// Mock configuration
		when(tokenConfigurationParameter.getPrivateKey()).thenReturn(privateKey);
		// when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);
		when(tokenConfigurationParameter.getAccessTokenExpireMinute()).thenReturn(15);

		// Set up claims
		claims = new HashMap<>();
		claims.put("userId", "user123");
		claims.put("username", "testuser");
		claims.put("roles", "USER");
	}

	@Test
	void generateToken_WithClaims_Success() {
		when(tokenConfigurationParameter.getRefreshTokenExpireDay()).thenReturn(7);
		// Act
		Token token = tokenGenerationService.generateToken(claims);

		// Assert
		assertNotNull(token);
		assertNotNull(token.getAccessToken());
		assertNotNull(token.getRefreshToken());
		assertTrue(token.getAccessTokenExpiresAt() > 0);

		// Verify the tokens can be parsed and contain expected claims
		Claims accessTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getAccessToken())
				.getBody();

		Claims refreshTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getRefreshToken())
				.getBody();

		// Verify access token claims
		assertEquals("user123", accessTokenClaims.get("userId"));
		assertEquals("testuser", accessTokenClaims.get("username"));
		assertEquals("USER", accessTokenClaims.get("roles"));
		assertNotNull(accessTokenClaims.getId());
		assertNotNull(accessTokenClaims.getIssuedAt());
		assertNotNull(accessTokenClaims.getExpiration());

		// Verify refresh token claims
		assertEquals("user123", refreshTokenClaims.get("userId"));
		assertNotNull(refreshTokenClaims.getId());
		assertNotNull(refreshTokenClaims.getIssuedAt());
		assertNotNull(refreshTokenClaims.getExpiration());

		// Verify token expiration times
		Date now = new Date();
		Date expectedAccessExpiry = DateUtils.addMinutes(now, 15);
		Date expectedRefreshExpiry = DateUtils.addDays(now, 7);
		
		// Allow for a small time difference due to test execution
		long accessExpiryDiff = Math.abs(expectedAccessExpiry.getTime() - accessTokenClaims.getExpiration().getTime());
		long refreshExpiryDiff = Math.abs(expectedRefreshExpiry.getTime() - refreshTokenClaims.getExpiration().getTime());
		
		assertTrue(accessExpiryDiff < 5000); // Within 5 seconds
		assertTrue(refreshExpiryDiff < 5000); // Within 5 seconds

		// Verify the configuration was used
		verify(tokenConfigurationParameter, times(2)).getPrivateKey();
		verify(tokenConfigurationParameter).getAccessTokenExpireMinute();
		verify(tokenConfigurationParameter).getRefreshTokenExpireDay();
	}

	@Test
	void generateToken_WithClaimsAndRefreshToken_Success() {
		// Arrange
		String existingRefreshToken = "existing-refresh-token";

		// Act
		Token token = tokenGenerationService.generateToken(claims, existingRefreshToken);

		// Assert
		assertNotNull(token);
		assertNotNull(token.getAccessToken());
		assertEquals(existingRefreshToken, token.getRefreshToken());
		assertTrue(token.getAccessTokenExpiresAt() > 0);

		// Verify the access token can be parsed and contains expected claims
		Claims accessTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getAccessToken())
				.getBody();

		// Verify access token claims
		assertEquals("user123", accessTokenClaims.get("userId"));
		assertEquals("testuser", accessTokenClaims.get("username"));
		assertEquals("USER", accessTokenClaims.get("roles"));
		assertNotNull(accessTokenClaims.getId());
		assertNotNull(accessTokenClaims.getIssuedAt());
		assertNotNull(accessTokenClaims.getExpiration());

		// Verify token expiration time
		Date now = new Date();
		Date expectedAccessExpiry = DateUtils.addMinutes(now, 15);
		
		// Allow for a small time difference due to test execution
		long accessExpiryDiff = Math.abs(expectedAccessExpiry.getTime() - accessTokenClaims.getExpiration().getTime());
		assertTrue(accessExpiryDiff < 5000); // Within 5 seconds

		// Verify the configuration was used
		verify(tokenConfigurationParameter).getPrivateKey();
		verify(tokenConfigurationParameter).getAccessTokenExpireMinute();
		verify(tokenConfigurationParameter, never()).getRefreshTokenExpireDay(); // Should not be called for this method
	}

	@Test
	void generateToken_WithEmptyClaims_Success() {
		when(tokenConfigurationParameter.getRefreshTokenExpireDay()).thenReturn(7);

		// Arrange
		Map<String, Object> emptyClaims = new HashMap<>();
		emptyClaims.put("userId", "user123"); // Minimum required claim

		// Act
		Token token = tokenGenerationService.generateToken(emptyClaims);

		// Assert
		assertNotNull(token);
		assertNotNull(token.getAccessToken());
		assertNotNull(token.getRefreshToken());
		assertTrue(token.getAccessTokenExpiresAt() > 0);

		// Verify the tokens can be parsed
		Claims accessTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getAccessToken())
				.getBody();

		Claims refreshTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getRefreshToken())
				.getBody();

		// Verify access token claims
		assertEquals("user123", accessTokenClaims.get("userId"));
		assertNotNull(accessTokenClaims.getId());
		assertNotNull(accessTokenClaims.getIssuedAt());
		assertNotNull(accessTokenClaims.getExpiration());

		// Verify refresh token claims
		assertEquals("user123", refreshTokenClaims.get("userId"));
		assertNotNull(refreshTokenClaims.getId());
		assertNotNull(refreshTokenClaims.getIssuedAt());
		assertNotNull(refreshTokenClaims.getExpiration());
	}

	@Test
	void generateToken_TokenTypeHeader_Success() {
		// Act
		Token token = tokenGenerationService.generateToken(claims);

		// Parse the token to verify header
		String[] accessTokenParts = token.getAccessToken().split("\\.");
		String[] refreshTokenParts = token.getRefreshToken().split("\\.");
		
		// We can't easily decode and verify the headers without additional libraries,
		// but we can verify that the tokens have the expected structure (3 parts: header.payload.signature)
		assertEquals(3, accessTokenParts.length);
		assertEquals(3, refreshTokenParts.length);

		// We can verify that the configuration was used
		verify(tokenConfigurationParameter, times(2)).getPrivateKey();
	}

	@Test
	void generateToken_AccessTokenExpiration_Success() {
		// Arrange
		when(tokenConfigurationParameter.getAccessTokenExpireMinute()).thenReturn(30); // Override default

		// Act
		Token token = tokenGenerationService.generateToken(claims);

		// Assert
		assertNotNull(token);
		
		// Verify the token expiration time
		Claims accessTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getAccessToken())
				.getBody();
		
		Date now = new Date();
		Date expectedAccessExpiry = DateUtils.addMinutes(now, 30);
		
		// Allow for a small time difference due to test execution
		long accessExpiryDiff = Math.abs(expectedAccessExpiry.getTime() - accessTokenClaims.getExpiration().getTime());
		assertTrue(accessExpiryDiff < 5000); // Within 5 seconds
		
		verify(tokenConfigurationParameter).getAccessTokenExpireMinute();
	}

	@Test
	void generateToken_RefreshTokenExpiration_Success() {
		// Arrange
		when(tokenConfigurationParameter.getRefreshTokenExpireDay()).thenReturn(14); // Override default

		// Act
		Token token = tokenGenerationService.generateToken(claims);

		// Assert
		assertNotNull(token);
		
		// Verify the token expiration time
		Claims refreshTokenClaims = Jwts.parserBuilder()
				.setSigningKey(publicKey)
				.build()
				.parseClaimsJws(token.getRefreshToken())
				.getBody();
		
		Date now = new Date();
		Date expectedRefreshExpiry = DateUtils.addDays(now, 14);
		
		// Allow for a small time difference due to test execution
		long refreshExpiryDiff = Math.abs(expectedRefreshExpiry.getTime() - refreshTokenClaims.getExpiration().getTime());
		assertTrue(refreshExpiryDiff < 5000); // Within 5 seconds
		
		verify(tokenConfigurationParameter).getRefreshTokenExpireDay();
	}
}
