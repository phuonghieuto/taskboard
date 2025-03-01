package com.phuonghieuto.backend.task_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.phuonghieuto.backend.task_service.config.FeignClientConfig;

/**
 * Feign client interface named {@link AuthServiceClient} for interacting with the User Service.
 * Provides methods to validate tokens and retrieve authentication information.
 */
@FeignClient(name = "auth-service", path = "/api/v1/auth", configuration = FeignClientConfig.class)
public interface AuthServiceClient {

    /**
     * Validates the provided token by making a request to the user service.
     *
     * @param token the token to validate
     */
    @PostMapping("/validate-token")
    void validateToken(@RequestParam String token);

    /**
     * Retrieves authentication information based on the provided token.
     *
     * @param token the token to use for retrieving authentication information
     * @return {@link UsernamePasswordAuthenticationToken} containing authentication details
     */
    @GetMapping("/authenticate")
    UsernamePasswordAuthenticationToken getAuthentication(@RequestParam String token);

}
