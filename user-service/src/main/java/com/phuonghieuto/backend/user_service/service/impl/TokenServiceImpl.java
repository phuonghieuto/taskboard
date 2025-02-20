package com.phuonghieuto.backend.user_service.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.phuonghieuto.backend.user_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.user_service.model.Token;
import com.phuonghieuto.backend.user_service.model.user.entity.InvalidTokenEntity;
import com.phuonghieuto.backend.user_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.user_service.model.user.enums.TokenType;
import com.phuonghieuto.backend.user_service.repository.InvalidTokenRepository;
import com.phuonghieuto.backend.user_service.service.TokenService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {
    private final TokenConfigurationParameter tokenConfigurationParameter;
    private final InvalidTokenRepository invalidTokenRepository;

    @Override
    public Token generateToken(final Map<String, Object> claims) {
        final long currentTimeMillis = System.currentTimeMillis();
        final Date tokenIssuedAt = new Date(currentTimeMillis);
        final Date accessTokenExpiresAt = DateUtils.addDays(new Date(currentTimeMillis),
                tokenConfigurationParameter.getAccessTokenExpireDay());

        final String accessToken = Jwts.builder()
                .setHeaderParam("type", TokenType.BEARER.getValue())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(tokenIssuedAt)
                .setExpiration(accessTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .addClaims(claims)
                .compact();

        final Date refreshTokenExpiresAt = DateUtils.addDays(new Date(currentTimeMillis),
                tokenConfigurationParameter.getRefreshTokenExpireDay());

        final String refreshToken = Jwts.builder()
                .setHeaderParam("type", TokenType.BEARER.getValue())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(tokenIssuedAt)
                .setExpiration(refreshTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .claim(TokenClaims.USER_ID.getValue(), claims.get(TokenClaims.USER_ID.getValue()))
                .compact();

        return Token.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public Token generateToken(final Map<String, Object> claims, final String refreshToken) {
        final long currentTimeMillis = System.currentTimeMillis();
        final String refreshTokenId = this.getId(refreshToken);
        this.checkForInvalidityOfToken(refreshTokenId);

        final Date accessTokenIssuedAt = new Date(currentTimeMillis);
        final Date accessTokenExpiresAt = DateUtils.addDays(new Date(currentTimeMillis),
                tokenConfigurationParameter.getAccessTokenExpireDay());

        final String accessToken = Jwts.builder()
                .setHeaderParam("type", TokenType.BEARER.getValue())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(accessTokenIssuedAt)
                .setExpiration(accessTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .addClaims(claims)
                .compact();

        return Token.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public UsernamePasswordAuthenticationToken getAuthentication(final String token) {
        final Jws<Claims> claimsJws = Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(token);

        final JwsHeader<?> jwsHeader = claimsJws.getHeader();
        final Claims payload = claimsJws.getBody();

        final Jwt jwt = new Jwt(
                token,
                payload.getIssuedAt().toInstant(),
                payload.getExpiration().toInstant(),
                Map.of(
                        TokenClaims.TYP.getValue(), jwsHeader.getType(),
                        TokenClaims.ALGORITHM.getValue(), jwsHeader.getAlgorithm()),
                payload);

        final String userType = payload.get(TokenClaims.USER_TYPE.getValue(), String.class);

        final ArrayList<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(userType));

        return new UsernamePasswordAuthenticationToken(jwt, null, authorities);
    }

    @Override
    public void verifyAndValidate(final String jwt) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(tokenConfigurationParameter.getPublicKey())
                    .build()
                    .parseClaimsJws(jwt);

            // Log the claims for debugging purposes
            Claims claims = claimsJws.getBody();
            log.info("Token claims: {}", claims);

            // Additional checks (e.g., expiration, issuer, etc.)
            if (claims.getExpiration().before(new Date())) {
                throw new JwtException("Token has expired");
            }

            log.info("Token is valid");

        } catch (ExpiredJwtException e) {
            log.error("Token has expired", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has expired", e);
        } catch (JwtException e) {
            log.error("Invalid JWT token", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token", e);
        } catch (Exception e) {
            log.error("Error validating token", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error validating token", e);
        }
    }

    @Override
    public void verifyAndValidate(final Set<String> jwts) {
        jwts.forEach(this::verifyAndValidate);
    }

    @Override
    public Jws<Claims> getClaims(final String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(jwt);
    }

    @Override
    public Claims getPayload(final String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    @Override
    public String getId(final String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody()
                .getId();
    }

    @Override
    public void invalidateTokens(Set<String> tokenIds) {
        final Set<InvalidTokenEntity> invalidTokenEntities = tokenIds.stream()
                .map(tokenId -> InvalidTokenEntity.builder()
                        .tokenId(tokenId)
                        .build())
                .collect(Collectors.toSet());

        invalidTokenRepository.saveAll(invalidTokenEntities);
    }

    @Override
    public void checkForInvalidityOfToken(String tokenId) {
        final boolean isTokenInvalid = invalidTokenRepository.findByTokenId(tokenId).isPresent();

        if (isTokenInvalid) {
            throw new TokenAlreadyInvalidatedException(tokenId);
        }
    }
}