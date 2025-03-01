package com.phuonghieuto.backend.auth_service.service;

import com.phuonghieuto.backend.auth_service.model.Token;

import java.util.Map;

public interface TokenGenerationService {
    Token generateToken(Map<String, Object> claims);
    Token generateToken(Map<String, Object> claims, String refreshToken);
}