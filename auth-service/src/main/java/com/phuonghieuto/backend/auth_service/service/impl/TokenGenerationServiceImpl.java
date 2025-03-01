package com.phuonghieuto.backend.auth_service.service.impl;

import com.phuonghieuto.backend.auth_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.auth_service.model.user.enums.TokenType;
import com.phuonghieuto.backend.auth_service.service.TokenGenerationService;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenGenerationServiceImpl implements TokenGenerationService {
        private final TokenConfigurationParameter tokenConfigurationParameter;

        @Override
        public Token generateToken(Map<String, Object> claims) {
                final long currentTimeMillis = System.currentTimeMillis();
                final Date tokenIssuedAt = new Date(currentTimeMillis);
                final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis),
                                tokenConfigurationParameter.getAccessTokenExpireMinute());

                final String accessToken = Jwts.builder()
                                .setHeaderParam(TokenClaims.TYP.getValue(), TokenType.BEARER.getValue())
                                .setId(UUID.randomUUID().toString())
                                .setIssuedAt(tokenIssuedAt)
                                .setExpiration(accessTokenExpiresAt)
                                .signWith(tokenConfigurationParameter.getPrivateKey())
                                .addClaims(claims)
                                .compact();

                final Date refreshTokenExpiresAt = DateUtils.addDays(new Date(currentTimeMillis),
                                tokenConfigurationParameter.getRefreshTokenExpireDay());

                final String refreshToken = Jwts.builder()
                                .setHeaderParam(TokenClaims.TYP.getValue(), TokenType.BEARER.getValue())
                                .setId(UUID.randomUUID().toString())
                                .setIssuedAt(tokenIssuedAt)
                                .setExpiration(refreshTokenExpiresAt)
                                .signWith(tokenConfigurationParameter.getPrivateKey())
                                .claim("userId", claims.get("userId"))
                                .compact();

                return Token.builder()
                                .accessToken(accessToken)
                                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                                .refreshToken(refreshToken)
                                .build();
        }

        @Override
        public Token generateToken(Map<String, Object> claims, String refreshToken) {
                final long currentTimeMillis = System.currentTimeMillis();
                final Date accessTokenIssuedAt = new Date(currentTimeMillis);
                final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis),
                                tokenConfigurationParameter.getAccessTokenExpireMinute());

                log.debug("Generating access token with claims: {}", claims);

                final String accessToken = Jwts.builder()
                                .setHeaderParam(TokenClaims.TYP.getValue(), TokenType.BEARER.getValue())
                                .setId(UUID.randomUUID().toString())
                                .setIssuedAt(accessTokenIssuedAt)
                                .setExpiration(accessTokenExpiresAt)
                                .signWith(tokenConfigurationParameter.getPrivateKey())
                                .addClaims(claims)
                                .compact();

                log.debug("Generated access token: {}", accessToken);

                return Token.builder()
                                .accessToken(accessToken)
                                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                                .refreshToken(refreshToken)
                                .build();
        }
}