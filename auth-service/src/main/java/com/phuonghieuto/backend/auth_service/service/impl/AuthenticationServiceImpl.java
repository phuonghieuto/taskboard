package com.phuonghieuto.backend.auth_service.service.impl;

import com.phuonghieuto.backend.auth_service.exception.PasswordNotValidException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.exception.UserStatusNotValidException;
import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.auth_service.model.user.enums.UserStatus;
import com.phuonghieuto.backend.auth_service.model.user.mapper.TokenToTokenResponseMapper;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import com.phuonghieuto.backend.auth_service.service.AuthenticationService;
import com.phuonghieuto.backend.auth_service.service.TokenGenerationService;
import com.phuonghieuto.backend.auth_service.service.TokenManagementService;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final TokenGenerationService tokenGenerationService;
        private final TokenValidationService tokenValidationService;
        private final TokenManagementService tokenManagementService;
        private final TokenToTokenResponseMapper tokenToTokenResponseMapper = TokenToTokenResponseMapper.initialize();

        @Override
        public TokenResponseDTO login(LoginRequestDTO loginRequest) {
                final UserEntity userEntityFromDB = userRepository.findUserEntityByEmail(loginRequest.getEmail())
                                .orElseThrow(() -> new UserNotFoundException(
                                                "Can't find with given email: " + loginRequest.getEmail()));
                if (Boolean.FALSE.equals(
                                passwordEncoder.matches(loginRequest.getPassword(), userEntityFromDB.getPassword()))) {
                        throw new PasswordNotValidException();
                }

                if (!userEntityFromDB.isEmailConfirmed()) {
                        throw new UserStatusNotValidException(
                                        "Email not confirmed. Please check your email to activate your account.");
                }

                validateUserStatus(userEntityFromDB);

                Token token = tokenGenerationService.generateToken(userEntityFromDB.getClaims());
                TokenResponseDTO tokenResponse = tokenToTokenResponseMapper.map(token);
                return tokenResponse;
        }

        @Override
        public TokenResponseDTO refreshToken(TokenRefreshRequestDTO tokenRefreshRequest) {

                tokenValidationService.verifyAndValidate(tokenRefreshRequest.getRefreshToken());

                final String userId = tokenValidationService.getPayload(tokenRefreshRequest.getRefreshToken())
                                .get(TokenClaims.USER_ID.getValue()).toString();

                final UserEntity userEntityFromDB = userRepository.findById(userId)
                                .orElseThrow(UserNotFoundException::new);

                validateUserStatus(userEntityFromDB);

                Token token = tokenGenerationService.generateToken(userEntityFromDB.getClaims(),
                                tokenRefreshRequest.getRefreshToken());

                TokenResponseDTO tokenResponse = tokenToTokenResponseMapper.map(token);
                return tokenResponse;
        }

        @Override
        public void logout(TokenInvalidateRequestDTO tokenInvalidateRequest) {
                tokenValidationService.verifyAndValidate(Set.of(tokenInvalidateRequest.getAccessToken(),
                                tokenInvalidateRequest.getRefreshToken()));

                final String accessTokenId = tokenValidationService.getPayload(tokenInvalidateRequest.getAccessToken())
                                .getId();

                tokenManagementService.checkForInvalidityOfToken(accessTokenId);

                final String refreshTokenId = tokenValidationService
                                .getPayload(tokenInvalidateRequest.getRefreshToken()).getId();

                tokenManagementService.checkForInvalidityOfToken(refreshTokenId);

                tokenManagementService.invalidateTokens(Set.of(accessTokenId, refreshTokenId));
        }

        private void validateUserStatus(final UserEntity userEntity) {
                if (!(UserStatus.ACTIVE.equals(userEntity.getUserStatus()))) {
                        throw new UserStatusNotValidException("UserStatus = " + userEntity.getUserStatus());
                }
        }
}