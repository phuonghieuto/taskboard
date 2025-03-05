package com.phuonghieuto.backend.api_gateway.config;

import java.util.List;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.phuonghieuto.backend.api_gateway.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;

        // Public endpoints that do not require authentication
        private static final List<String> PUBLIC_ENDPOINTS = List.of("/api/v1/users/register", "/api/v1/auth/login",
                        "/api/v1/auth/refresh-token", "/api/v1/auth/logout", "/api/v1/auth/validate-token",
                        "/api/v1/auth/authenticate");

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                return builder.routes().route("auth-service", r -> r.path("/api/v1/auth/**")
                                .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                .uri("lb://auth-service"))
                                .route("auth-service", r -> r.path("/api/v1/users/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://auth-service"))
                                .route("task-service", r -> r.path("/api/v1/tasks/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://task-service"))
                                .route("task-service", r -> r.path("/api/v1/boards/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://task-service"))
                                .route("task-service", r -> r.path("/api/v1/tables/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://task-service"))
                                .route("task-service", r -> r.path("/api/v1/tasks/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://task-service"))
                                .build();
        }
}
