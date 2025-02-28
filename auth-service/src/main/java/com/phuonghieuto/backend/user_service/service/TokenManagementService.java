package com.phuonghieuto.backend.user_service.service;

import java.util.Set;

public interface TokenManagementService {
    void invalidateTokens(Set<String> tokenIds);
    void checkForInvalidityOfToken(String tokenId);
}