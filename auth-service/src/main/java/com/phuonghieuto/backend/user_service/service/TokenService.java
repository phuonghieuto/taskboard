package com.phuonghieuto.backend.user_service.service;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public interface TokenService {
    UsernamePasswordAuthenticationToken getAuthentication(String token);
}