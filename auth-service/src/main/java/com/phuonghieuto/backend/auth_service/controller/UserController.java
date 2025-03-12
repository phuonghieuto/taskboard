package com.phuonghieuto.backend.auth_service.controller;

import com.phuonghieuto.backend.auth_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.UserEmailDTO;
import com.phuonghieuto.backend.auth_service.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "APIs for user registration and management")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Creates a new user account in the system with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/register"
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "status": 400,
                        "message": "Validation failed",
                        "details": {
                          "email": "must be a well-formed email address",
                          "password": "size must be between 8 and 30",
                          "firstName": "must not be blank"
                        }
                      },
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/register"
                    }
                    """))),
            @ApiResponse(responseCode = "409", description = "Email already in use", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "status": 409,
                        "message": "Email already in use",
                        "details": null
                      },
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/register"
                    }
                    """))) })
    @PostMapping("/register")
    public CustomResponse<Void> registerUser(
            @RequestBody @Validated @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "User registration details", required = true, content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterRequestDTO.class), examples = @ExampleObject(name = "registerExample", summary = "Standard user registration", value = """
                    {
                      "email": "john.doe@example.com",
                      "password": "Password123!",
                      "username": "johndoe",
                      "firstName": "John",
                      "lastName": "Doe",
                      "phoneNumber": "1234567890"
                    }
                    """))) final RegisterRequestDTO registerRequest) {

        log.info("UserController | registerUser");
        userService.registerUser(registerRequest);
        return CustomResponse.SUCCESS;
    }

    @Operation(summary = "Get user email", description = "Retrieves the email address associated with the specified user ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserEmailDTO.class), examples = @ExampleObject(value = """
                    {
                      "userId": "1234567890",
                      "email": "john.doe@example.com"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "User not found",
                        "details": null
                      },
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/1234567890/email"
                    }
                    """))) })
    @GetMapping("/{userId}/email")
    public UserEmailDTO getUserEmail(@PathVariable String userId) {
        log.info("UserController | getUserEmail | userId: {}", userId);
        return userService.getUserEmail(userId);
    }

    @Operation(summary = "Get user ID by email", description = "Retrieves the user ID associated with the specified email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User ID retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserEmailDTO.class), examples = @ExampleObject(value = """
                    {
                      "userId": "1234567890",
                      "email": "john.doe@example.com"
                    }
                    """))),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "User not found with email: john.doe@example.com",
                        "details": null
                      },
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/by-email?email=john.doe@example.com"
                    }
                    """))) })
    @GetMapping("/by-email")
    public UserEmailDTO getUserIdFromEmail(@RequestParam String email) {
        log.info("UserController | getUserIdFromEmail | email: {}", email);
        return userService.getUserIdFromEmail(email);
    }

    @Operation(summary = "Confirm user email", description = "Confirms a user's email address using the confirmation token sent to their email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email confirmed successfully", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/confirm-email"
                    }
                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                    {
                      "success": false,
                      "error": {
                        "status": 400,
                        "message": "Invalid confirmation token",
                        "details": null
                      },
                      "timestamp": "2024-05-25T14:30:00.000Z",
                      "path": "/api/v1/users/confirm-email"
                    }
                    """))) })
    @GetMapping("/confirm-email")
    public CustomResponse<Void> confirmEmail(@RequestParam String token) {
        log.info("UserController | confirmEmail | token: {}", token);
        userService.confirmEmail(token);
        return CustomResponse.SUCCESS;
    }
}