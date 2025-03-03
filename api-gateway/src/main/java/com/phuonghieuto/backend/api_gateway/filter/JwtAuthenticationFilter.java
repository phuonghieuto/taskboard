package com.phuonghieuto.backend.api_gateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.phuonghieuto.backend.api_gateway.client.AuthServiceClient;
import com.phuonghieuto.backend.api_gateway.model.Token;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * A custom Gateway filter named {@link JwtAuthenticationFilter} that handles JWT authentication for requests.
 * This filter validates JWT tokens for all requests except those to public endpoints.
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    /**
     * Configuration class for JwtAuthenticationFilter.
     * It holds a list of public endpoints that should not be filtered.
     */
    public static class Config {
        // List of public endpoints that should not be filtered
        private List<String> publicEndpoints;

        /**
         * Gets the list of public endpoints.
         *
         * @return the list of public endpoints
         */
        public List<String> getPublicEndpoints() {
            return publicEndpoints;
        }

        /**
         * Sets the list of public endpoints.
         *
         * @param publicEndpoints the list of public endpoints to set
         * @return the updated Config object
         */
        public Config setPublicEndpoints(List<String> publicEndpoints) {
            this.publicEndpoints = publicEndpoints;
            return this;
        }
    }

    /**
     * Applies the JWT authentication filter to the gateway.
     *
     * @param config the configuration for the filter
     * @return the gateway filter
     */
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

                // Inject authServiceClient here
                ApplicationContext context = exchange.getApplicationContext();
                AuthServiceClient authServiceClient = context.getBean(AuthServiceClient.class);

                return Mono.fromCallable(() -> {
                            authServiceClient.validateToken(jwt);
                            log.debug("Token validation succeeded for path: {}", path);
                            return true;
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(valid -> chain.filter(exchange))
                        .onErrorResume(e -> {
                            log.error("Token validation failed for path: {}: {}", path, e.getMessage());
                            
                            HttpStatus status = HttpStatus.UNAUTHORIZED; // Default to 401 for auth errors
                            String errorMessage = "Authentication failed";
                            String errorHeader = "AUTH_ERROR";
                            
                            // Handle different types of exceptions
                            if (e instanceof FeignException) {
                                FeignException feignException = (FeignException) e;
                                int statusCode = feignException.status();
                                
                                // Map specific status codes from auth-service
                                if (statusCode == 401 || statusCode == 403) {
                                    // Token is expired or invalid
                                    status = HttpStatus.UNAUTHORIZED;
                                    errorMessage = "Token is expired or invalid";
                                } else if (statusCode == 400) {
                                    // Bad request - likely invalid token format
                                    status = HttpStatus.BAD_REQUEST;
                                    errorMessage = "Invalid token format";
                                } else if (statusCode == 404) {
                                    // Not found - likely endpoint not found
                                    status = HttpStatus.UNAUTHORIZED;  // Override 404 with 401 for security
                                    errorMessage = "Authentication failed";
                                } else {
                                    // Other error codes
                                    status = HttpStatus.valueOf(statusCode);
                                    errorMessage = "Authentication error";
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
                                    } catch (Exception ex) {
                                        log.warn("Could not parse error response: {}", errorBody);
                                    }
                                }
                            } else {
                                // Unexpected error
                                log.error("Unexpected error during token validation", e);
                                status = HttpStatus.INTERNAL_SERVER_ERROR;
                                errorMessage = "Unexpected authentication error";
                            }

                            // Always log the actual status being returned to client
                            log.debug("Returning status {} to client with message: {}", status, errorMessage);

                            // Set response status
                            exchange.getResponse().setStatusCode(status);
                            exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

                            // Create error response body
                            ObjectNode errorResponse = objectMapper.createObjectNode();
                            errorResponse.put("header", errorHeader);
                            errorResponse.put("isSuccess", false);
                            errorResponse.put("message", errorMessage);
                            errorResponse.put("httpStatus", status.value());

                            byte[] responseBytes;
                            try {
                                responseBytes = objectMapper.writeValueAsBytes(errorResponse);
                            } catch (Exception jsonException) {
                                log.error("Error serializing error response", jsonException);
                                responseBytes = "{\"error\":\"Authentication error\"}".getBytes();
                            }

                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
                            return exchange.getResponse().writeWith(Mono.just(buffer));
                        });
            }
            
            // No token provided but required
            log.warn("Missing or invalid Authorization header for path: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            
            // Create error response for missing token
            ObjectNode errorResponse = objectMapper.createObjectNode();
            errorResponse.put("header", "AUTH_ERROR");
            errorResponse.put("isSuccess", false);
            errorResponse.put("message", "Authentication required");
            errorResponse.put("httpStatus", HttpStatus.UNAUTHORIZED.value());
            
            try {
                byte[] responseBytes = objectMapper.writeValueAsBytes(errorResponse);
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(responseBytes);
                return exchange.getResponse().writeWith(Mono.just(buffer));
            } catch (Exception jsonException) {
                log.error("Error creating error response", jsonException);
                return exchange.getResponse().setComplete();
            }
        };
    }
}