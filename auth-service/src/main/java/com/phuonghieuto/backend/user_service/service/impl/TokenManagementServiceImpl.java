package com.phuonghieuto.backend.user_service.service.impl;

import com.phuonghieuto.backend.user_service.model.user.entity.InvalidTokenEntity;
import com.phuonghieuto.backend.user_service.repository.InvalidTokenRepository;
import com.phuonghieuto.backend.user_service.service.TokenManagementService;
import com.phuonghieuto.backend.user_service.exception.TokenAlreadyInvalidatedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TokenManagementServiceImpl implements TokenManagementService {
    private final InvalidTokenRepository invalidTokenRepository;

    @Override
    public void invalidateTokens(Set<String> tokenIds) {
        final Set<InvalidTokenEntity> invalidTokenEntities = tokenIds.stream()
                .map(tokenId -> InvalidTokenEntity.builder()
                        .tokenId(tokenId)
                        .build())
                .collect(Collectors.toSet());

        invalidTokenRepository.saveAll(invalidTokenEntities);
    }

    @Override
    public void checkForInvalidityOfToken(String tokenId) {
        final boolean isTokenInvalid = invalidTokenRepository.findByTokenId(tokenId).isPresent();

        if (isTokenInvalid) {
            throw new TokenAlreadyInvalidatedException(tokenId);
        }
    }
}