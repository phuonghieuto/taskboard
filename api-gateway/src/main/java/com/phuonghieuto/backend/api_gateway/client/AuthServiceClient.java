package com.phuonghieuto.backend.api_gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

//Feign client for the Auth Service
@FeignClient(name = "auth-service", path = "/api/v1/auth")
public interface AuthServiceClient {

    // validate token by sending a request to the Auth Service
    // If the token is invalid, the Auth Service will throw an exception
    @PostMapping("/validate-token")
    void validateToken(@RequestParam String token);

}
