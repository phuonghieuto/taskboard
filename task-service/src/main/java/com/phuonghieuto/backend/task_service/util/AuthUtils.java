package com.phuonghieuto.backend.task_service.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.phuonghieuto.backend.task_service.client.AuthServiceClient;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthUtils {
    private final AuthServiceClient authServiceClient;

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaim(TokenClaims.USER_ID.getValue());
        }
        throw new UnauthorizedAccessException("User not authenticated");
    }

    public String getUserIdFromEmail(String email) {
        try {
            return authServiceClient.getUserIdFromEmail(email).getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    public String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaim(TokenClaims.USER_EMAIL.getValue());
        }
        throw new UnauthorizedAccessException("User not authenticated");
    }
}