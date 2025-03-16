package com.phuonghieuto.backend.auth_service.service;

import java.util.Set;

public interface TokenManagementService {
    void invalidateTokens(Set<String> tokenIds);
    boolean checkForInvalidityOfToken(String tokenId);
}