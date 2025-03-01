package com.phuonghieuto.backend.auth_service.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public interface TokenService {
    UsernamePasswordAuthenticationToken getAuthentication(String token);
}