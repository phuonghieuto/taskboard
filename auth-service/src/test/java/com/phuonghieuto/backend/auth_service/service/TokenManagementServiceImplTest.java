package com.phuonghieuto.backend.auth_service.service;

import com.phuonghieuto.backend.auth_service.exception.TokenAlreadyInvalidatedException;
import com.phuonghieuto.backend.auth_service.model.user.entity.InvalidTokenEntity;
import com.phuonghieuto.backend.auth_service.repository.InvalidTokenRepository;
import com.phuonghieuto.backend.auth_service.service.impl.TokenManagementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenManagementServiceImplTest {

    @Mock
    private InvalidTokenRepository invalidTokenRepository;

    @InjectMocks
    private TokenManagementServiceImpl tokenManagementService;

    @Captor
    private ArgumentCaptor<Set<InvalidTokenEntity>> invalidTokenEntitiesCaptor;

    private Set<String> tokenIds;

    @BeforeEach
    void setUp() {
        tokenIds = new HashSet<>();
        tokenIds.add("token-id-1");
        tokenIds.add("token-id-2");
        tokenIds.add("token-id-3");
    }

    @Test
    void invalidateTokens_Success() {
        // Arrange
        when(invalidTokenRepository.saveAll(any())).thenReturn(null);

        // Act
        tokenManagementService.invalidateTokens(tokenIds);

        // Assert
        verify(invalidTokenRepository).saveAll(invalidTokenEntitiesCaptor.capture());

        Set<InvalidTokenEntity> capturedEntities = invalidTokenEntitiesCaptor.getValue();
        assertEquals(3, capturedEntities.size());

        // Convert the captured entities back to a set of token IDs for comparison
        Set<String> capturedTokenIds = capturedEntities.stream().map(InvalidTokenEntity::getTokenId)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(capturedTokenIds.contains("token-id-1"));
        assertTrue(capturedTokenIds.contains("token-id-2"));
        assertTrue(capturedTokenIds.contains("token-id-3"));
    }

    @Test
    void invalidateTokens_EmptySet() {
        // Arrange
        Set<String> emptyTokenIds = new HashSet<>();

        // Act
        tokenManagementService.invalidateTokens(emptyTokenIds);

        // Assert
        verify(invalidTokenRepository).saveAll(invalidTokenEntitiesCaptor.capture());

        Set<InvalidTokenEntity> capturedEntities = invalidTokenEntitiesCaptor.getValue();
        assertTrue(capturedEntities.isEmpty());
    }

    @Test
    void checkForInvalidityOfToken_ValidToken() {
        // Arrange
        when(invalidTokenRepository.findByTokenId(anyString())).thenReturn(Optional.empty());

        // Act
        boolean result = tokenManagementService.checkForInvalidityOfToken("valid-token-id");

        // Assert
        assertFalse(result);
        verify(invalidTokenRepository).findByTokenId("valid-token-id");
    }

    @Test
    void checkForInvalidityOfToken_InvalidToken() {
        // Arrange
        InvalidTokenEntity invalidTokenEntity = new InvalidTokenEntity();
        invalidTokenEntity.setTokenId("invalid-token-id");

        when(invalidTokenRepository.findByTokenId("invalid-token-id")).thenReturn(Optional.of(invalidTokenEntity));

        // Act & Assert
        assertThrows(TokenAlreadyInvalidatedException.class,
                () -> tokenManagementService.checkForInvalidityOfToken("invalid-token-id"));

        verify(invalidTokenRepository).findByTokenId("invalid-token-id");
    }

    @Test
    void checkForInvalidityOfToken_NullTokenId() {
        // Arrange
        when(invalidTokenRepository.findByTokenId(null)).thenReturn(Optional.empty());

        // Act
        boolean result = tokenManagementService.checkForInvalidityOfToken(null);

        // Assert
        assertFalse(result);
        verify(invalidTokenRepository).findByTokenId(null);
    }

    @Test
    void invalidateTokens_WithDuplicates() {
        // Arrange
        Set<String> tokensWithDuplicates = new HashSet<>();
        tokensWithDuplicates.add("token-id-1");
        tokensWithDuplicates.add("token-id-1"); // Duplicate, but HashSet will remove it
        tokensWithDuplicates.add("token-id-2");

        when(invalidTokenRepository.saveAll(any())).thenReturn(null);

        // Act
        tokenManagementService.invalidateTokens(tokensWithDuplicates);

        // Assert
        verify(invalidTokenRepository).saveAll(invalidTokenEntitiesCaptor.capture());

        Set<InvalidTokenEntity> capturedEntities = invalidTokenEntitiesCaptor.getValue();
        assertEquals(2, capturedEntities.size()); // Only 2 unique token IDs

        // Convert the captured entities back to a set of token IDs for comparison
        Set<String> capturedTokenIds = capturedEntities.stream().map(InvalidTokenEntity::getTokenId)
                .collect(java.util.stream.Collectors.toSet());

        assertTrue(capturedTokenIds.contains("token-id-1"));
        assertTrue(capturedTokenIds.contains("token-id-2"));
    }

    @Test
    void invalidateTokens_RepositoryException() {
        // Arrange
        when(invalidTokenRepository.saveAll(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> tokenManagementService.invalidateTokens(tokenIds));
        verify(invalidTokenRepository).saveAll(any());
    }
}
