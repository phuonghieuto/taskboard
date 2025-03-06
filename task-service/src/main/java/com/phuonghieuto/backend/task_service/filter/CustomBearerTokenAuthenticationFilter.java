package com.phuonghieuto.backend.task_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.phuonghieuto.backend.task_service.model.auth.Token;
import com.phuonghieuto.backend.task_service.service.TokenService;

import java.io.IOException;

/**
 * Custom filter for handling Bearer token authentication in HTTP requests. Uses
 * a hybrid approach with local validation and remote invalidation check.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpServletRequest,
            @NonNull final HttpServletResponse httpServletResponse, @NonNull final FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("CustomBearerTokenAuthenticationFilter: Request received for URI: {}",
                httpServletRequest.getRequestURI());

        final String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (Token.isBearerToken(authorizationHeader)) {
            final String jwt = Token.getJwt(authorizationHeader);

            try {
                // Step 1: Local validation (signature, expiration)
                tokenService.validateToken(jwt);
                log.debug("Token validation succeeded for request: {}", httpServletRequest.getRequestURI());

                // Step 2: Get authentication from local token parsing
                final UsernamePasswordAuthenticationToken authentication = tokenService.getAuthentication(jwt);

                // Set authentication to SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ResponseStatusException e) {
                log.error("Local token validation failed: {}", e.getMessage());
                httpServletResponse.setStatus(e.getStatusCode().value());
                httpServletResponse.getWriter().write(e.getReason());
                return;
            } catch (Exception e) {
                log.error("Token processing failed: {}", e.getMessage());
                httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                httpServletResponse.getWriter().write("Internal server error during token processing");
                return;
            }
        } else {
            log.warn("Missing or invalid Authorization header for request: {}", httpServletRequest.getRequestURI());
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}