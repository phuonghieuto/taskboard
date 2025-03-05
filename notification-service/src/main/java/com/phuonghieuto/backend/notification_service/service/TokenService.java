package com.phuonghieuto.backend.notification_service.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public interface TokenService {
    void validateToken(String token);
    UsernamePasswordAuthenticationToken getAuthentication(String token);
}
