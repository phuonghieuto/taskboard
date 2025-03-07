package com.phuonghieuto.backend.auth_service.controller;

import com.phuonghieuto.backend.auth_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;
import com.phuonghieuto.backend.auth_service.service.AuthenticationService;
import com.phuonghieuto.backend.auth_service.service.TokenService;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication, token management, and validation")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final TokenValidationService tokenValidationService;
    private final TokenService tokenService;
    
    @Operation(
        summary = "Login a user", 
        description = "Authenticates a user with their credentials and returns access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User logged in successfully", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = TokenResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "accessToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjEyMzQ1Njc4OTAiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsInVzZXJGaXJzdE5hbWUiOiJKb2huIiwidXNlckxhc3ROYW1lIjoiRG9lIiwidXNlckVtYWlsIjoiam9obi5kb2VAZXhhbXBsZS5jb20iLCJpYXQiOjE3MTY2MTk1NTAsImV4cCI6MTcxNjYyMDQ1MH0.signature",
                        "accessTokenExpiresAt": 1716620450000,
                        "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjI0NjgxMDEyMTQiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsImlhdCI6MTcxNjYxOTU1MCwiZXhwIjoxNzE2NjY0MjMwfQ.signature"
                      },
                      "timestamp": "2024-05-25T12:34:56.789Z",
                      "path": "/api/v1/auth/login"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials", 
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 401,
                        "message": "Invalid username or password",
                        "details": null
                      },
                      "timestamp": "2024-05-25T12:34:56.789Z",
                      "path": "/api/v1/auth/login"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/login")
    public CustomResponse<TokenResponseDTO> loginUser(
            @RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "User login credentials",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = LoginRequestDTO.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "email": "john.doe@example.com",
                          "password": "Password123!"
                        }
                        """
                    )
                )
            ) final LoginRequestDTO loginRequest) {
        
        log.info("AuthController | loginUser");
        final TokenResponseDTO tokenResponse = authenticationService.login(loginRequest);
        return CustomResponse.successOf(tokenResponse);
    }

    @Operation(
        summary = "Refresh a token", 
        description = "Generates new access and refresh tokens using a valid refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Token refreshed successfully", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = TokenResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "accessToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6Ijg3NjU0MzIxMCIsInVzZXJJZCI6IjEyMzQ1Njc4OTAiLCJ1c2VyVHlwZSI6IlVTRVIiLCJ1c2VyU3RhdHVzIjoiQUNUSVZFIiwidXNlckZpcnN0TmFtZSI6IkpvaG4iLCJ1c2VyTGFzdE5hbWUiOiJEb2UiLCJ1c2VyRW1haWwiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTcxNjYyMTAwMCwiZXhwIjoxNzE2NjIxOTAwfQ.signature",
                        "accessTokenExpiresAt": 1716621900000,
                        "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjU0NzY5ODMyMSIsInVzZXJJZCI6IjEyMzQ1Njc4OTAiLCJ1c2VyVHlwZSI6IlVTRVIiLCJ1c2VyU3RhdHVzIjoiQUNUSVZFIiwiaWF0IjoxNzE2NjIxMDAwLCJleHAiOjE3MTY2NjU2ODB9.signature"
                      },
                      "timestamp": "2024-05-25T13:10:00.000Z",
                      "path": "/api/v1/auth/refresh-token"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid refresh token", 
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 401,
                        "message": "Invalid or expired refresh token",
                        "details": null
                      },
                      "timestamp": "2024-05-25T13:10:00.000Z",
                      "path": "/api/v1/auth/refresh-token"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/refresh-token")
    public CustomResponse<TokenResponseDTO> refreshToken(
            @RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Refresh token request",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = TokenRefreshRequestDTO.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjI0NjgxMDEyMTQiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsImlhdCI6MTcxNjYxOTU1MCwiZXhwIjoxNzE2NjY0MjMwfQ.signature"
                        }
                        """
                    )
                )
            ) final TokenRefreshRequestDTO tokenRefreshRequest) {
        
        log.info("AuthController | refreshToken");
        final TokenResponseDTO tokenResponse = authenticationService.refreshToken(tokenRefreshRequest);
        return CustomResponse.successOf(tokenResponse);
    }

    @Operation(
        summary = "Logout a user", 
        description = "Invalidates the user's access and refresh tokens to prevent further use"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User logged out successfully", 
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "timestamp": "2024-05-25T13:15:00.000Z",
                      "path": "/api/v1/auth/logout"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid token", 
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 401,
                        "message": "Invalid or expired token",
                        "details": null
                      },
                      "timestamp": "2024-05-25T13:15:00.000Z",
                      "path": "/api/v1/auth/logout"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/logout")
    public CustomResponse<Void> logout(
            @RequestBody @Valid @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Token invalidation request",
                required = true,
                content = @Content(
                    schema = @Schema(implementation = TokenInvalidateRequestDTO.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "accessToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjEyMzQ1Njc4OTAiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsInVzZXJGaXJzdE5hbWUiOiJKb2huIiwidXNlckxhc3ROYW1lIjoiRG9lIiwidXNlckVtYWlsIjoiam9obi5kb2VAZXhhbXBsZS5jb20iLCJpYXQiOjE3MTY2MTk1NTAsImV4cCI6MTcxNjYyMDQ1MH0.signature",
                          "refreshToken": "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjI0NjgxMDEyMTQiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsImlhdCI6MTcxNjYxOTU1MCwiZXhwIjoxNzE2NjY0MjMwfQ.signature"
                        }
                        """
                    )
                )
            ) final TokenInvalidateRequestDTO tokenInvalidateRequest) {
        
        log.info("AuthController | logout");
        authenticationService.logout(tokenInvalidateRequest);
        return CustomResponse.SUCCESS;
    }

    @Operation(
        summary = "Validate a token", 
        description = "Checks if a provided token is valid and not expired or invalidated"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Token is valid", 
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid token", 
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "status": 401,
                      "message": "Invalid or expired token",
                      "timestamp": "2024-05-25T13:20:00.000Z",
                      "path": "/api/v1/auth/validate-token"
                    }
                    """
                )
            )
        )
    })
    @PostMapping("/validate-token")
    public ResponseEntity<Void> validateToken(
            @Parameter(
                name = "token", 
                description = "JWT token to validate", 
                required = true,
                in = ParameterIn.QUERY,
                example = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjEyMzQ1Njc4OTAiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsInVzZXJGaXJzdE5hbWUiOiJKb2huIiwidXNlckxhc3ROYW1lIjoiRG9lIiwidXNlckVtYWlsIjoiam9obi5kb2VAZXhhbXBsZS5jb20iLCJpYXQiOjE3MTY2MTk1NTAsImV4cCI6MTcxNjYyMDQ1MH0.signature"
            ) @RequestParam String token) {
        
        log.info("AuthController | validateToken");
        tokenValidationService.verifyAndValidate(token);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Get authentication details", 
        description = "Extracts and returns the authentication details from a valid JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication details retrieved successfully", 
            content = @Content(
                mediaType = "application/json", 
                schema = @Schema(implementation = UsernamePasswordAuthenticationToken.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "principal": {
                        "tokenValue": "eyJhbGciOiJSUzI1NiJ9...",
                        "issuedAt": "2024-05-25T12:34:56.789Z",
                        "expiresAt": "2024-05-25T13:34:56.789Z",
                        "headers": {
                          "alg": "RS256",
                          "typ": "JWT"
                        },
                        "claims": {
                          "sub": "john.doe@example.com",
                          "userId": "1234567890",
                          "userType": "USER",
                          "userStatus": "ACTIVE",
                          "iat": 1716619550,
                          "exp": 1716620450
                        }
                      },
                      "credentials": null,
                      "authenticated": true,
                      "authorities": [
                        {
                          "authority": "USER"
                        }
                      ]
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid token", 
            content = @Content(
                examples = @ExampleObject(
                    value = """
                    {
                      "status": 401,
                      "message": "Invalid or expired token",
                      "timestamp": "2024-05-25T13:25:00.000Z",
                      "path": "/api/v1/auth/authenticate"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/authenticate")
    public ResponseEntity<UsernamePasswordAuthenticationToken> getAuthentication(
            @Parameter(
                name = "token", 
                description = "JWT token to extract authentication details from", 
                required = true,
                in = ParameterIn.QUERY,
                example = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ0YXNrLW1hbmFnZW1lbnQiLCJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImp0aSI6IjEyMzQ1Njc4OTAiLCJ1c2VySWQiOiIxMjM0NTY3ODkwIiwidXNlclR5cGUiOiJVU0VSIiwidXNlclN0YXR1cyI6IkFDVElWRSIsInVzZXJGaXJzdE5hbWUiOiJKb2huIiwidXNlckxhc3ROYW1lIjoiRG9lIiwidXNlckVtYWlsIjoiam9obi5kb2VAZXhhbXBsZS5jb20iLCJpYXQiOjE3MTY2MTk1NTAsImV4cCI6MTcxNjYyMDQ1MH0.signature"
            ) @RequestParam String token) {
        
        log.info("AuthController | authenticate");
        UsernamePasswordAuthenticationToken authentication = tokenService.getAuthentication(token);
        log.info("AuthController | authenticate | authentication: {}", authentication);
        return ResponseEntity.ok(authentication);
    }
}