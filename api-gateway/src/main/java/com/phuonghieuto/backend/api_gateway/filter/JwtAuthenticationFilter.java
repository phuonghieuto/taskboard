package com.phuonghieuto.backend.api_gateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.phuonghieuto.backend.api_gateway.client.AuthServiceClient;
import com.phuonghieuto.backend.api_gateway.model.Token;
import com.phuonghieuto.backend.api_gateway.model.common.CustomError;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

// Custom Gateway filter to authenticate requests using JWT tokens
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ObjectMapper objectMapper;
    private final AuthServiceClient authServiceClient;

    public JwtAuthenticationFilter(AuthServiceClient authServiceClient) {
        super(Config.class);
        this.authServiceClient = authServiceClient;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public static class Config {
        // List of public endpoints that should not be filtered
        private List<String> publicEndpoints;

        public List<String> getPublicEndpoints() {
            return publicEndpoints;
        }

        public Config setPublicEndpoints(List<String> publicEndpoints) {
            this.publicEndpoints = publicEndpoints;
            return this;
        }
    }

    // Apply jwt authentication filter to incoming requests
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip filtering for public endpoints
            if (config != null && config.getPublicEndpoints().stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange);
            }

            String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (Token.isBearerToken(authorizationHeader)) {
                String jwt = Token.getJwt(authorizationHeader);

                // Validate token using auth-service
                return Mono.fromCallable(() -> {
                            authServiceClient.validateToken(jwt);
                            log.debug("Token validation succeeded for path: {}", path);
                            return true;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(valid -> chain.filter(exchange))
                        .onErrorResume(e -> {
                            log.error("Token validation failed for path: {}: {}", path, e.getMessage());
                            
                            HttpStatus status;
                            String errorMessage;
                            String errorHeader = CustomError.Header.AUTH_ERROR.getName();
                            
                            // Handle different types of exceptions
                            if (e instanceof FeignException feignException) {
                                int statusCode = feignException.status();
                                
                                // Map specific status codes from auth-service
                                switch (statusCode) {
                                    case 401, 403 -> {
                                        // Token is expired or invalid
                                        status = HttpStatus.UNAUTHORIZED;
                                        errorMessage = "Token is expired or invalid";
                                    }
                                    case 400 -> {
                                        // Bad request - likely invalid token format
                                        status = HttpStatus.BAD_REQUEST;
                                        errorMessage = "Invalid token format";
                                    }
                                    case 404 -> {
                                        // Not found - likely endpoint not found
                                        status = HttpStatus.UNAUTHORIZED;  // Override 404 with 401 for security
                                        errorMessage = "Unauthorized";
                                    }
                                    default -> {
                                        // Other error codes
                                        status = HttpStatus.valueOf(statusCode);
                                        errorMessage = "Authentication error";
                                    }
                                }
                                
                                // Try to extract more specific error message if available
                                String errorBody = feignException.contentUTF8();
                                if (errorBody != null && !errorBody.isEmpty()) {
                                    try {
                                        ObjectNode jsonNode = objectMapper.readValue(errorBody, ObjectNode.class);
                                        if (jsonNode.has("message")) {
                                            errorMessage = jsonNode.get("message").asText();
                                        }
                                        if (jsonNode.has("header")) {
                                            errorHeader = jsonNode.get("header").asText();
                                        }
                                    } catch (JsonProcessingException ex) {
                                        log.warn("Could not parse error response: {}", errorBody);
                                    }
                                }
                            } else {
                                // Unexpected error
                                log.error("Unexpected error during token validation", e);
                                status = HttpStatus.INTERNAL_SERVER_ERROR;
                                errorMessage = "Unexpected authentication error";
                            }

                            log.debug("Returning status {} to client with message: {}", status, errorMessage);
                            
                            CustomError customError = CustomError.builder()
                                .httpStatus(status)
                                .header(errorHeader)
                                .message(errorMessage)
                                .build();
                            
                            return createErrorResponse(exchange.getResponse(), customError);
                        });
            }
            
            // No token provided but required
            log.warn("Missing or invalid Authorization header for path: {}", path);
            
            CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.UNAUTHORIZED)
                .header(CustomError.Header.AUTH_ERROR.getName())
                .message("Authentication required")
                .build();
                
            return createErrorResponse(exchange.getResponse(), customError);
        };
    }
    
    /**
     * Creates a standardized error response with the CustomError format
     *
     * @param response The ServerHttpResponse to write to
     * @param customError The error details
     * @return Mono<Void> representing the completion of the response write
     */
    private Mono<Void> createErrorResponse(ServerHttpResponse response, CustomError customError) {
        response.setStatusCode(customError.getHttpStatus());
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        try {
            byte[] responseBytes = objectMapper.writeValueAsBytes(customError);
            DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException jsonException) {
            log.error("Error creating error response", jsonException);
            return response.setComplete();
        }
    }
}