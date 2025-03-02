package com.phuonghieuto.backend.task_service.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.phuonghieuto.backend.task_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.task_service.service.TokenService;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {
    private final TokenConfigurationParameter tokenConfigurationParameter;

    @Override
    public void validateToken(String token) {
        try {
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(tokenConfigurationParameter.getPublicKey())
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();

            // Check if token is expired
            if (claims.getExpiration().before(new Date())) {
                throw new JwtException("Token has expired");
            }

            log.debug("Token is valid");

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
    public UsernamePasswordAuthenticationToken getAuthentication(String token) {
        log.debug("TokenServiceImpl | getAuthentication | token: {}", token);
        try {
            validateToken(token);
            
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