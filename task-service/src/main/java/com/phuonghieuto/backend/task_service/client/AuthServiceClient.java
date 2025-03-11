package com.phuonghieuto.backend.task_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.phuonghieuto.backend.task_service.model.auth.dto.UserEmailDTO;


//Feign client for the Auth Service
@FeignClient(name = "auth-service", path = "/api/v1/users")
public interface AuthServiceClient {

    @GetMapping("/{userId}/email")
    UserEmailDTO getUserEmail(@PathVariable("userId") String userId);

    @GetMapping("/by-email")
    UserEmailDTO getUserIdFromEmail(@RequestParam("email") String email);
}
