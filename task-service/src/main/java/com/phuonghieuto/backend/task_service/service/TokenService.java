package com.phuonghieuto.backend.task_service.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public interface TokenService {
    /**
     * Validates a JWT token.
     * 
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    void validateToken(String token);
    
    /**
     * Gets authentication details from a JWT token.
     * 
     * @param token the JWT token to authenticate
     * @return authentication details
     */
    UsernamePasswordAuthenticationToken getAuthentication(String token);
}