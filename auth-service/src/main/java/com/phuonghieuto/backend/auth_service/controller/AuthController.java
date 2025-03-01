package com.phuonghieuto.backend.auth_service.controller;

import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;
import com.phuonghieuto.backend.auth_service.model.user.mapper.TokenToTokenResponseMapper;
import com.phuonghieuto.backend.auth_service.service.AuthenticationService;
import com.phuonghieuto.backend.auth_service.service.TokenService;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class AuthController {

    private final AuthenticationService authenticationService;
    private final TokenValidationService tokenValidationService;
    private final TokenService tokenService;
    private final TokenToTokenResponseMapper tokenToTokenResponseMapper = TokenToTokenResponseMapper.initialize();

    @Operation(summary = "Login a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged in successfully", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content)
    })
    @PostMapping("/login")
    public CustomResponse<TokenResponseDTO> loginUser(@RequestBody @Valid final LoginRequestDTO loginRequest) {
        log.info("AuthController | loginUser");
        final Token token = authenticationService.login(loginRequest);
        final TokenResponseDTO tokenResponse = tokenToTokenResponseMapper.map(token);
        return CustomResponse.successOf(tokenResponse);
    }

    @Operation(summary = "Refresh a token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token", content = @Content)
    })
    @PostMapping("/refresh-token")
    public CustomResponse<TokenResponseDTO> refreshToken(@RequestBody @Valid final TokenRefreshRequestDTO tokenRefreshRequest) {
        log.info("AuthController | refreshToken");
        final Token token = authenticationService.refreshToken(tokenRefreshRequest);
        final TokenResponseDTO tokenResponse = tokenToTokenResponseMapper.map(token);
        return CustomResponse.successOf(tokenResponse);
    }

    @Operation(summary = "Logout a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User logged out successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content)
    })
    @PostMapping("/logout")
    public CustomResponse<Void> logout(@RequestBody @Valid final TokenInvalidateRequestDTO tokenInvalidateRequest) {
        log.info("AuthController | logout");
        authenticationService.logout(tokenInvalidateRequest);
        return CustomResponse.SUCCESS;
    }

    @Operation(summary = "Validate a token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid", content = @Content),
            @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content)
    })
    @PostMapping("/validate-token")
    public ResponseEntity<Void> validateToken(@RequestParam String token) {
        log.info("AuthController | validateToken");
        tokenValidationService.verifyAndValidate(token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get authentication details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication details retrieved successfully", content = @Content(schema = @Schema(implementation = UsernamePasswordAuthenticationToken.class))),
            @ApiResponse(responseCode = "401", description = "Invalid token", content = @Content)
    })
    @GetMapping("/authenticate")
    public ResponseEntity<UsernamePasswordAuthenticationToken> getAuthentication(@RequestParam String token) {
        log.info("AuthController | authenticate");
        UsernamePasswordAuthenticationToken authentication = tokenService.getAuthentication(token);
        log.info("AuthController | authenticate | authentication: {}", authentication);
        return ResponseEntity.ok(authentication);
    }
}