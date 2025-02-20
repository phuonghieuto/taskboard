package com.phuonghieuto.backend.user_service.controller;

import com.phuonghieuto.backend.user_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.user_service.model.user.dto.UserCreateDTO;
import com.phuonghieuto.backend.user_service.model.user.dto.UserDTO;
import com.phuonghieuto.backend.user_service.model.user.dto.UserUpdateDTO;
import com.phuonghieuto.backend.user_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.user_service.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public CustomResponse<Void> registerUser(@RequestBody @Validated final RegisterRequestDTO registerRequest) {
        log.info("UserController | registerUser");
        userService.registerUser(registerRequest);
        return CustomResponse.SUCCESS;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<UserDTO> user = userService.getUserById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO userUpdateDTO) {
        UserDTO updatedUser = userService.updateUser(id, userUpdateDTO);
        return ResponseEntity.ok(updatedUser);
    }
}