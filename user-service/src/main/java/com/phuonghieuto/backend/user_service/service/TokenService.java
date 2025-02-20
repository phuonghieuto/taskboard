package com.phuonghieuto.backend.user_service.service;

import java.util.Map;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.phuonghieuto.backend.user_service.model.Token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public interface TokenService {
    Token generateToken(final Map<String, Object> claims);
    Token generateToken(final Map<String, Object> claims, final String refreshToken);
    UsernamePasswordAuthenticationToken getAuthentication(final String token);
    void verifyAndValidate(final String jwt);
    void verifyAndValidate(final Set<String> jwts);
    Jws<Claims> getClaims(final String jwt);
    Claims getPayload(final String jwt);
    String getId(final String jwt);
    void invalidateTokens(final Set<String> tokenIds);
    void checkForInvalidityOfToken(final String tokenId);
}
