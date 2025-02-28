package com.phuonghieuto.backend.user_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.util.Set;

public interface TokenValidationService {
    void verifyAndValidate(String jwt);
    void verifyAndValidate(Set<String> jwts);
    Jws<Claims> getClaims(String jwt);
    Claims getPayload(String jwt);
    String getId(String jwt);
}