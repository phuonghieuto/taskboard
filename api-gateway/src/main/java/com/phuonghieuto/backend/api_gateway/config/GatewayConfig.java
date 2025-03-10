package com.phuonghieuto.backend.api_gateway.config;

import java.util.Arrays;
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
        private final List<String> PUBLIC_ENDPOINTS = Arrays.asList("/api/v1/users/register", "/api/v1/users/*/email",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/v1/auth/api-docs",
                        "/api/v1/tasks/api-docs", "/api/v1/notifications/api-docs", "/api/v1/ws-notifications/**");

        @Bean
        public RouteLocator routes(RouteLocatorBuilder builder) {
                return builder.routes().route("auth-service", r -> r.path("/api/v1/auth/**").uri("lb://auth-service"))
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
                                .route("notification-service", r -> r.path("/api/v1/notifications/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://notification-service"))
                                .route("notification-service", r -> r.path("/api/v1/preferences/**").filters(
                                                f -> f.filter(jwtAuthFilter.apply(new JwtAuthenticationFilter.Config()
                                                                .setPublicEndpoints(PUBLIC_ENDPOINTS))))
                                                .uri("lb://notification-service"))
                                .route("notification-websocket", r -> r.path("/api/v1/ws-notifications/**")
                                                .uri("lb:ws://notification-service"))
                                .build();
        }
}
