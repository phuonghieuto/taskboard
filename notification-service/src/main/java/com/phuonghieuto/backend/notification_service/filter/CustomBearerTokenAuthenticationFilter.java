package com.phuonghieuto.backend.notification_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import com.phuonghieuto.backend.notification_service.model.auth.Token;
import com.phuonghieuto.backend.notification_service.service.TokenService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomBearerTokenAuthenticationFilter extends OncePerRequestFilter {
    
    private final TokenService tokenService;
    
    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpServletRequest,
                                   @NonNull final HttpServletResponse httpServletResponse,
                                   @NonNull final FilterChain filterChain) throws ServletException, IOException {
        log.debug("API Request was secured with Security!");
        final String authHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        
        if (!StringUtils.hasText(authHeader) || !Token.isBearerToken(authHeader)) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        
        try {
            final String jwt = Token.getJwt(authHeader);
            
            // Validate the token locally
            tokenService.validateToken(jwt);
            
            final UsernamePasswordAuthenticationToken authentication = tokenService.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
        } catch (ResponseStatusException e) {
            log.error("Token validation failed: {}", e.getMessage());
            httpServletResponse.setStatus(e.getStatusCode().value());
            httpServletResponse.getWriter().write(e.getReason());
            return;
        } catch (Exception e) {
            log.error("Token processing failed: {}", e.getMessage());
            httpServletResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            httpServletResponse.getWriter().write("Internal server error during authentication");
            return;
        }
        
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}