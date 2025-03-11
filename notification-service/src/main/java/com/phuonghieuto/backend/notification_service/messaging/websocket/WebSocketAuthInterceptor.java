package com.phuonghieuto.backend.notification_service.messaging.websocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.phuonghieuto.backend.notification_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.notification_service.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Get token from query parameters
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        
        if (token == null) {
            log.warn("No token provided in WebSocket connection");
            return false;
        }
        
        try {
            // Validate token using your existing token service
            tokenService.validateToken(token);
            
                        // Extract userId from authentication
            UsernamePasswordAuthenticationToken authentication = tokenService.getAuthentication(token);
            String userId = null;
            
            // The principal is a Jwt object
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                // Extract userId from claims
                userId = jwt.getClaim(TokenClaims.USER_ID.getValue()).toString();
            } else {
                // Fallback if principal is not a Jwt object
                userId = principal.toString();
            }
            
            // Store userId in attributes for later use in the handler
            attributes.put("userId", userId);
            
            log.debug("WebSocket authentication successful for user: {}", userId);
            return true;
        } catch (Exception e) {
            log.error("WebSocket authentication failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do here
    }
}