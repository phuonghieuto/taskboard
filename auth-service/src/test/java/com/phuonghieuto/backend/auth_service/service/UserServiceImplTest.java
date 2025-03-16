package com.phuonghieuto.backend.auth_service.service;

import com.phuonghieuto.backend.auth_service.exception.UserAlreadyExistException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.messaging.producer.NotificationProducer;
import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.UserEmailDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserStatus;
import com.phuonghieuto.backend.auth_service.model.user.mapper.RegisterRequestToUserEntityMapper;
import com.phuonghieuto.backend.auth_service.model.user.mapper.UserEntityToUserMapper;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import com.phuonghieuto.backend.auth_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RegisterRequestToUserEntityMapper registerRequestToUserEntityMapper;

    @Mock
    private UserEntityToUserMapper userEntityToUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationProducer notificationProducer;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequestDTO registerRequest;
    private UserEntity userEntity;
    private User user;

    @BeforeEach
    void setUp() {

        // Set up test data
        registerRequest = new RegisterRequestDTO();
        registerRequest.setEmail("test@example.com");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password");

        userEntity = spy(new UserEntity());
        userEntity.setId("user123");
        userEntity.setEmail("test@example.com");
        userEntity.setUsername("testuser");
        userEntity.setPassword("encodedPassword");
        userEntity.setUserStatus(UserStatus.PASSIVE);
        userEntity.setEmailConfirmed(false);

        user = new User();
        user.setId("user123");
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setUserStatus(UserStatus.PASSIVE);
        user.setEmailConfirmed(false);
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.existsUserEntityByEmail(anyString())).thenReturn(false);
        when(userRepository.existsUserEntityByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(registerRequestToUserEntityMapper.mapForSaving(any(RegisterRequestDTO.class))).thenReturn(userEntity);
        when(userEntityToUserMapper.map(any(UserEntity.class))).thenReturn(user);
        // Act
        User result = userService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("testuser", result.getUsername());
        assertEquals(UserStatus.PASSIVE.name(), result.getUserStatus().name());
        assertFalse(result.isEmailConfirmed());

        verify(userRepository).existsUserEntityByEmail("test@example.com");
        verify(userRepository).existsUserEntityByUsername("testuser");
        verify(passwordEncoder).encode("password");
        verify(userRepository).save(userEntity);
        verify(notificationProducer).sendEmailConfirmationMessage(userEntity);
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsUserEntityByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistException.class, () -> userService.registerUser(registerRequest));
        verify(userRepository).existsUserEntityByEmail("test@example.com");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void registerUser_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsUserEntityByEmail(anyString())).thenReturn(false);
        when(userRepository.existsUserEntityByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(UserAlreadyExistException.class, () -> userService.registerUser(registerRequest));
        verify(userRepository).existsUserEntityByEmail("test@example.com");
        verify(userRepository).existsUserEntityByUsername("testuser");
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void getUserEmail_Success() {
        // Arrange
        when(userRepository.findById("user123")).thenReturn(Optional.of(userEntity));

        // Act
        UserEmailDTO result = userService.getUserEmail("user123");

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserEmail_UserNotFound() {
        // Arrange
        when(userRepository.findById("nonExistentId")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUserEmail("nonExistentId"));
    }

    @Test
    void getUserIdFromEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(userEntity));

        // Act
        UserEmailDTO result = userService.getUserIdFromEmail("test@example.com");

        // Assert
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserIdFromEmail_EmailNotFound() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getUserIdFromEmail("nonexistent@example.com"));
    }

    @Test
    void confirmEmail_Success() {
        // Arrange
        UserEntity userToConfirm = new UserEntity();
        userToConfirm.setId("user123");
        userToConfirm.setEmail("test@example.com");
        userToConfirm.setConfirmationToken("valid-token");
        userToConfirm.setConfirmationTokenExpiry(LocalDateTime.now().plusDays(1));
        userToConfirm.setEmailConfirmed(false);
        userToConfirm.setUserStatus(UserStatus.PASSIVE);

        when(userRepository.findByConfirmationToken("valid-token")).thenReturn(Optional.of(userToConfirm));
        when(userRepository.save(any(UserEntity.class))).thenReturn(userToConfirm);

        // Act
        boolean result = userService.confirmEmail("valid-token");

        // Assert
        assertTrue(result);
        assertTrue(userToConfirm.isEmailConfirmed());
        assertEquals(UserStatus.ACTIVE, userToConfirm.getUserStatus());
        assertNull(userToConfirm.getConfirmationToken());
        assertNull(userToConfirm.getConfirmationTokenExpiry());
        verify(userRepository).save(userToConfirm);
    }

    @Test
    void confirmEmail_InvalidToken() {
        // Arrange
        when(userRepository.findByConfirmationToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.confirmEmail("invalid-token"));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void confirmEmail_ExpiredToken() {
        // Arrange
        UserEntity userWithExpiredToken = new UserEntity();
        userWithExpiredToken.setConfirmationToken("expired-token");
        userWithExpiredToken.setConfirmationTokenExpiry(LocalDateTime.now().minusDays(1));

        when(userRepository.findByConfirmationToken("expired-token")).thenReturn(Optional.of(userWithExpiredToken));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.confirmEmail("expired-token"));
        verify(userRepository, never()).save(any(UserEntity.class));
    }
}