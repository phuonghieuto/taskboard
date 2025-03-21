package com.phuonghieuto.backend.auth_service.service.impl;

import com.phuonghieuto.backend.auth_service.config.TokenConfigurationParameter;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;
import com.phuonghieuto.backend.auth_service.service.TokenManagementService;
import com.phuonghieuto.backend.auth_service.exception.TokenAlreadyInvalidatedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationServiceImpl implements TokenValidationService {
    private final TokenConfigurationParameter tokenConfigurationParameter;
    private final TokenManagementService tokenManagementService;
    @Override
    public boolean verifyAndValidate(String jwt) {
        try {
            tokenManagementService.checkForInvalidityOfToken(getId(jwt));
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(tokenConfigurationParameter.getPublicKey())
                    .build()
                    .parseClaimsJws(jwt);

            Claims claims = claimsJws.getBody();

            // Additional checks (e.g., expiration, issuer, etc.)
            if (claims.getExpiration().before(new Date())) {
                throw new JwtException("Token has expired");
            }

            log.info("Token is valid");
            return true;

        } catch (ExpiredJwtException e) {
            log.error("Token has expired", e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has expired", e);
        } catch (JwtException e) {
            log.error("Invalid JWT token", e);
            throw new JwtException("Invalid JWT token");
        } catch (TokenAlreadyInvalidatedException e) {
            log.error("Token is already invalidated", e);
            throw new TokenAlreadyInvalidatedException();
        } catch (Exception e) {
            log.error("Error validating token", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error validating token", e);
        }
    }

    @Override
    public boolean verifyAndValidate(Set<String> jwts) {
        for (String jwt : jwts) {
            if (!verifyAndValidate(jwt)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Jws<Claims> getClaims(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(jwt);
    }

    @Override
    public Claims getPayload(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody();
    }

    @Override
    public String getId(String jwt) {
        return Jwts.parserBuilder()
                .setSigningKey(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody()
                .getId();
    }
}