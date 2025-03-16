package com.phuonghieuto.backend.auth_service.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

import java.util.Set;

public interface TokenValidationService {
    boolean verifyAndValidate(String jwt);
    boolean verifyAndValidate(Set<String> jwts);
    Jws<Claims> getClaims(String jwt);
    Claims getPayload(String jwt);
    String getId(String jwt);
}