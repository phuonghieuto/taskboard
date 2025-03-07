package com.phuonghieuto.backend.auth_service.service;

import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;

public interface AuthenticationService {
    TokenResponseDTO login(LoginRequestDTO loginRequest);
    TokenResponseDTO refreshToken(TokenRefreshRequestDTO tokenRefreshRequest);
    void logout(TokenInvalidateRequestDTO tokenInvalidateRequest);
}