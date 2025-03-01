package com.phuonghieuto.backend.auth_service.filter;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.service.TokenManagementService;
import com.phuonghieuto.backend.auth_service.service.TokenService;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomBearerTokenAuthenticationFilter extends OncePerRequestFilter {
    private final TokenValidationService tokenValidationService;
    private final TokenManagementService tokenManagementService;
    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest httpServletRequest,
            @NonNull final HttpServletResponse httpServletResponse,
            @NonNull final FilterChain filterChain) throws ServletException, IOException {

        log.debug("API Request was secured with Security!");

        final String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (Token.isBearerToken(authorizationHeader)) {

            final String jwt = Token.getJwt(authorizationHeader);

            tokenValidationService.verifyAndValidate(jwt);

            final String tokenId = tokenValidationService.getId(jwt);

            tokenManagementService.checkForInvalidityOfToken(tokenId);

            final UsernamePasswordAuthenticationToken authentication = tokenService
                    .getAuthentication(jwt);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);

    }
}
