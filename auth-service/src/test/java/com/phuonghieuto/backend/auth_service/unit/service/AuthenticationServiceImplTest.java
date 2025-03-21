package com.phuonghieuto.backend.auth_service.unit.service;

import com.phuonghieuto.backend.auth_service.exception.PasswordNotValidException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.exception.UserStatusNotValidException;
import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserStatus;
import com.phuonghieuto.backend.auth_service.model.user.mapper.TokenToTokenResponseMapper;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import com.phuonghieuto.backend.auth_service.service.TokenGenerationService;
import com.phuonghieuto.backend.auth_service.service.TokenManagementService;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;
import com.phuonghieuto.backend.auth_service.service.impl.AuthenticationServiceImpl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenGenerationService tokenGenerationService;

    @Mock
    private TokenValidationService tokenValidationService;

    @Mock
    private TokenManagementService tokenManagementService;

    @Mock
    private TokenToTokenResponseMapper tokenToTokenResponseMapper;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private UserEntity activeUser;
    private UserEntity passiveUser;
    private UserEntity userWithUnconfirmedEmail;
    private LoginRequestDTO loginRequest;
    private Token token;
    private TokenResponseDTO tokenResponseDTO;

    @BeforeEach
    void setUp() {
        // Set up active user
        activeUser = new UserEntity();
        activeUser.setId("user123");
        activeUser.setEmail("active@example.com");
        activeUser.setPassword("encodedPassword");
        activeUser.setEmailConfirmed(true);
        activeUser.setUserStatus(UserStatus.ACTIVE);

        // Set up passive user
        passiveUser = new UserEntity();
        passiveUser.setId("user456");
        passiveUser.setEmail("passive@example.com");
        passiveUser.setPassword("encodedPassword");
        passiveUser.setEmailConfirmed(true);
        passiveUser.setUserStatus(UserStatus.PASSIVE);

        // Set up user with unconfirmed email
        userWithUnconfirmedEmail = new UserEntity();
        userWithUnconfirmedEmail.setId("user789");
        userWithUnconfirmedEmail.setEmail("unconfirmed@example.com");
        userWithUnconfirmedEmail.setPassword("encodedPassword");
        userWithUnconfirmedEmail.setEmailConfirmed(false);
        userWithUnconfirmedEmail.setUserStatus(UserStatus.PASSIVE);

        // Set up login request
        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("active@example.com");
        loginRequest.setPassword("password123");

        // Set up token
        token = new Token();
        token.setAccessToken("access-token");
        token.setRefreshToken("refresh-token");

        // Set up token response
        tokenResponseDTO = new TokenResponseDTO();
        tokenResponseDTO.setAccessToken("access-token");
        tokenResponseDTO.setRefreshToken("refresh-token");
    }

    @Test
    void login_Success() {
        // Arrange
        when(userRepository.findUserEntityByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(tokenGenerationService.generateToken(any())).thenReturn(token);
        when(tokenToTokenResponseMapper.map(token)).thenReturn(tokenResponseDTO);

        // Act
        TokenResponseDTO result = authenticationService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());

        verify(userRepository).findUserEntityByEmail("active@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(tokenGenerationService).generateToken(any());
        verify(tokenToTokenResponseMapper).map(token);
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(userRepository.findUserEntityByEmail("active@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> authenticationService.login(loginRequest));

        verify(userRepository).findUserEntityByEmail("active@example.com");
        verifyNoInteractions(tokenGenerationService);
        verifyNoInteractions(tokenToTokenResponseMapper);
    }

    @Test
    void login_PasswordNotValid() {
        // Arrange
        when(userRepository.findUserEntityByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(PasswordNotValidException.class, () -> authenticationService.login(loginRequest));

        verify(userRepository).findUserEntityByEmail("active@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verifyNoInteractions(tokenGenerationService);
        verifyNoInteractions(tokenToTokenResponseMapper);
    }

    @Test
    void login_EmailNotConfirmed() {
        // Arrange
        loginRequest.setEmail("unconfirmed@example.com");
        when(userRepository.findUserEntityByEmail("unconfirmed@example.com"))
                .thenReturn(Optional.of(userWithUnconfirmedEmail));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        assertThrows(UserStatusNotValidException.class, () -> authenticationService.login(loginRequest));

        verify(userRepository).findUserEntityByEmail("unconfirmed@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verifyNoInteractions(tokenGenerationService);
        verifyNoInteractions(tokenToTokenResponseMapper);
    }

    @Test
    void login_UserStatusNotActive() {
        // Arrange
        loginRequest.setEmail("passive@example.com");
        when(userRepository.findUserEntityByEmail("passive@example.com")).thenReturn(Optional.of(passiveUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        // Act & Assert
        assertThrows(UserStatusNotValidException.class, () -> authenticationService.login(loginRequest));

        verify(userRepository).findUserEntityByEmail("passive@example.com");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verifyNoInteractions(tokenGenerationService);
        verifyNoInteractions(tokenToTokenResponseMapper);
    }

    @Test
    void refreshToken_Success() {
        // Arrange
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("valid-refresh-token");

        Claims payload = Jwts.claims();
        payload.put(TokenClaims.USER_ID.getValue(), "user123");

        when(tokenValidationService.verifyAndValidate(anyString())).thenReturn(true);
        when(tokenValidationService.getPayload("valid-refresh-token")).thenReturn(payload);
        when(userRepository.findById("user123")).thenReturn(Optional.of(activeUser));
        when(tokenGenerationService.generateToken(any(), anyString())).thenReturn(token);
        when(tokenToTokenResponseMapper.map(token)).thenReturn(tokenResponseDTO);

        // Act
        TokenResponseDTO result = authenticationService.refreshToken(refreshRequest);

        // Assert
        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());

        verify(tokenValidationService).verifyAndValidate("valid-refresh-token");
        verify(tokenValidationService).getPayload("valid-refresh-token");
        verify(userRepository).findById("user123");
        verify(tokenGenerationService).generateToken(any(), eq("valid-refresh-token"));
        verify(tokenToTokenResponseMapper).map(token);
    }

    @Test
    void refreshToken_UserNotFound() {
        // Arrange
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("valid-refresh-token");

        Claims payload = Jwts.claims();
        payload.put(TokenClaims.USER_ID.getValue(), "nonexistent-user");

        when(tokenValidationService.verifyAndValidate(anyString())).thenReturn(true);
        when(tokenValidationService.getPayload("valid-refresh-token")).thenReturn(payload);
        when(userRepository.findById("nonexistent-user")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> authenticationService.refreshToken(refreshRequest));

        verify(tokenValidationService).verifyAndValidate("valid-refresh-token");
        verify(tokenValidationService).getPayload("valid-refresh-token");
        verify(userRepository).findById("nonexistent-user");
        verifyNoInteractions(tokenToTokenResponseMapper);
    }

    @Test
    void refreshToken_UserStatusNotActive() {
        // Arrange
        TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
        refreshRequest.setRefreshToken("valid-refresh-token");

        Claims payload = Jwts.claims();
        payload.put(TokenClaims.USER_ID.getValue(), "user456");

        when(tokenValidationService.verifyAndValidate(anyString())).thenReturn(true);
        when(tokenValidationService.getPayload("valid-refresh-token")).thenReturn(payload);
        when(userRepository.findById("user456")).thenReturn(Optional.of(passiveUser));

        // Act & Assert
        assertThrows(UserStatusNotValidException.class, () -> authenticationService.refreshToken(refreshRequest));

        verify(tokenValidationService).verifyAndValidate("valid-refresh-token");
        verify(tokenValidationService).getPayload("valid-refresh-token");
        verify(userRepository).findById("user456");
        verifyNoInteractions(tokenGenerationService);
        verifyNoInteractions(tokenToTokenResponseMapper);
    }

    @Test
    void logout_Success() {
        // Arrange
        TokenInvalidateRequestDTO invalidateRequest = new TokenInvalidateRequestDTO();
        invalidateRequest.setAccessToken("access-token");
        invalidateRequest.setRefreshToken("refresh-token");

        Claims accessPayload = Jwts.claims();
        accessPayload.setId("access-token-id");

        Claims refreshPayload = Jwts.claims();
        refreshPayload.setId("refresh-token-id");

        when(tokenValidationService.verifyAndValidate(anySet())).thenReturn(true);
        when(tokenValidationService.getPayload("access-token")).thenReturn(accessPayload);
        when(tokenValidationService.getPayload("refresh-token")).thenReturn(refreshPayload);

        // Use doNothing with anyString() to match any string argument
        doReturn(false).when(tokenManagementService).checkForInvalidityOfToken(anyString());
        doNothing().when(tokenManagementService).invalidateTokens(anySet());

        // Act
        authenticationService.logout(invalidateRequest);

        // Assert
        verify(tokenValidationService).verifyAndValidate(Set.of("access-token", "refresh-token"));
        verify(tokenValidationService).getPayload("access-token");
        verify(tokenValidationService).getPayload("refresh-token");
        verify(tokenManagementService).checkForInvalidityOfToken("access-token-id");
        verify(tokenManagementService).checkForInvalidityOfToken("refresh-token-id");
        verify(tokenManagementService).invalidateTokens(Set.of("access-token-id", "refresh-token-id"));
    }

    @Test
    void validateUserStatus_Active() {
        // This is a private method, but we can test it indirectly through the public
        // methods
        // Already tested in login_Success and refreshToken_Success
    }

    @Test
    void validateUserStatus_NotActive() {
        // This is a private method, but we can test it indirectly through the public
        // methods
        // Already tested in login_UserStatusNotActive and
        // refreshToken_UserStatusNotActive
    }
}