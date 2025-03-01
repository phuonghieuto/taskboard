package com.phuonghieuto.backend.auth_service.service;

import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;

public interface AuthenticationService {
    Token login(LoginRequestDTO loginRequest);
    Token refreshToken(TokenRefreshRequestDTO tokenRefreshRequest);
    void logout(TokenInvalidateRequestDTO tokenInvalidateRequest);
}