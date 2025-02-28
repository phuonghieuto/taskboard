package com.phuonghieuto.backend.user_service.service.impl;

import com.phuonghieuto.backend.user_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.user_service.service.TokenValidationService;
import com.phuonghieuto.backend.user_service.model.user.enums.TokenClaims;
import com.phuonghieuto.backend.user_service.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {
        private final TokenConfigurationParameter tokenConfigurationParameter;
        private final TokenValidationService tokenValidationService;

        @Override
        public UsernamePasswordAuthenticationToken getAuthentication(final String token) {
                log.debug("TokenServiceImpl | getAuthentication | token: {}", token);
                try {
                        tokenValidationService.verifyAndValidate(token);
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
                } catch (Exception e) {
                        log.error("TokenServiceImpl | getAuthentication | Error parsing token: {}", e.getMessage(), e);
                        throw new RuntimeException("Invalid token", e);
                }
        }
}