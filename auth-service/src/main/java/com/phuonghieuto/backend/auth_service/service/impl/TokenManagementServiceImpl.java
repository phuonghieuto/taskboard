package com.phuonghieuto.backend.auth_service.service.impl;

import com.phuonghieuto.backend.auth_service.model.user.entity.InvalidTokenEntity;
import com.phuonghieuto.backend.auth_service.repository.InvalidTokenRepository;
import com.phuonghieuto.backend.auth_service.service.TokenManagementService;
import com.phuonghieuto.backend.auth_service.exception.TokenAlreadyInvalidatedException;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManagementServiceImpl implements TokenManagementService {
    private final InvalidTokenRepository invalidTokenRepository;

    @Override
    @CacheEvict(value = "invalidTokens", allEntries = true)
    public void invalidateTokens(Set<String> tokenIds) {
        final Set<InvalidTokenEntity> invalidTokenEntities = tokenIds.stream()
                .map(tokenId -> InvalidTokenEntity.builder()
                        .tokenId(tokenId)
                        .build())
                .collect(Collectors.toSet());

        invalidTokenRepository.saveAll(invalidTokenEntities);
    }

    @Override
    @Cacheable(value = "invalidTokens", key = "#tokenId")
    public boolean checkForInvalidityOfToken(String tokenId) {
        log.info("Checking for invalidity of token with ID: {}", tokenId);
        final boolean isTokenInvalid = invalidTokenRepository.findByTokenId(tokenId).isPresent();

        if (isTokenInvalid) {
            throw new TokenAlreadyInvalidatedException();
        }

        return isTokenInvalid;
    }
}