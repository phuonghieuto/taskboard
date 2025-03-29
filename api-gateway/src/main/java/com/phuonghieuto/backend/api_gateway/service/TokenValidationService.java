package com.phuonghieuto.backend.api_gateway.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.api_gateway.client.AuthServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final AuthServiceClient authServiceClient;

    /**
     * Validates a JWT token and caches the result
     * 
     * @param token The JWT token to validate
     * @return true if token is valid, exception thrown otherwise
     */
    @Cacheable(value = "tokenValidation", key = "#token")
    public boolean validateToken(String token) {
        log.debug("Token validation cache miss, calling auth service");
        authServiceClient.validateToken(token);
        return true;
    }
}