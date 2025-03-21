package com.phuonghieuto.backend.auth_service.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.auth_service.controller.AuthController;
import com.phuonghieuto.backend.auth_service.exception.PasswordNotValidException;
import com.phuonghieuto.backend.auth_service.exception.TokenAlreadyInvalidatedException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.exception.UserStatusNotValidException;
import com.phuonghieuto.backend.auth_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.auth_service.model.common.CustomError;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.LoginRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenInvalidateRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.TokenRefreshRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.TokenResponseDTO;
import com.phuonghieuto.backend.auth_service.service.AuthenticationService;
import com.phuonghieuto.backend.auth_service.service.TokenService;
import com.phuonghieuto.backend.auth_service.service.TokenValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Collections;
import java.util.Date;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        private MockMvc mockMvc;

        @Mock
        private AuthenticationService authenticationService;

        @Mock
        private TokenValidationService tokenValidationService;

        @Mock
        private TokenService tokenService;

        @InjectMocks
        private AuthController authController;

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                // Create a controller advice instance for global exception handling
                GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

                // Configure MockMvc with the controller and exception handler
                mockMvc = MockMvcBuilders.standaloneSetup(authController).setControllerAdvice(exceptionHandler).build();

                objectMapper = new ObjectMapper();
        }

        @Test
        void loginUser_Success() throws Exception {
                // Arrange
                LoginRequestDTO loginRequest = new LoginRequestDTO();
                loginRequest.setEmail("test@example.com");
                loginRequest.setPassword("Password123!");

                TokenResponseDTO tokenResponse = new TokenResponseDTO();
                tokenResponse.setAccessToken("valid-access-token");
                tokenResponse.setRefreshToken("valid-refresh-token");
                tokenResponse.setAccessTokenExpiresAt(new Date().getTime() + 3600000);

                when(authenticationService.login(any(LoginRequestDTO.class))).thenReturn(tokenResponse);

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andDo(result -> {
                                        System.out.println(
                                                        "Response JSON: " + result.getResponse().getContentAsString());
                                });

                // Assert
                resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.httpStatus").value("OK"))
                                .andExpect(jsonPath("$.response.accessToken").value("valid-access-token"))
                                .andExpect(jsonPath("$.response.refreshToken").value("valid-refresh-token"));

                verify(authenticationService).login(any(LoginRequestDTO.class));
        }

        @Test
        void loginUser_InvalidCredentials() throws Exception {
                // Arrange
                LoginRequestDTO loginRequest = new LoginRequestDTO();
                loginRequest.setEmail("test@example.com");
                loginRequest.setPassword("WrongPassword123!");

                when(authenticationService.login(any(LoginRequestDTO.class)))
                                .thenThrow(new PasswordNotValidException("Invalid username or password"));

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)));

                // Assert
                resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.VALIDATION_ERROR.getName()));

                verify(authenticationService).login(any(LoginRequestDTO.class));
        }

        @Test
        void loginUser_UserNotFound() throws Exception {
                // Arrange
                LoginRequestDTO loginRequest = new LoginRequestDTO();
                loginRequest.setEmail("nonexistent@example.com");
                loginRequest.setPassword("Password123!");

                when(authenticationService.login(any(LoginRequestDTO.class))).thenThrow(
                                new UserNotFoundException("User not found with email: " + loginRequest.getEmail()));

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)));

                // Assert
                resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message")
                                                .value("User not found with email: " + loginRequest.getEmail()))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.NOT_FOUND.getName()));

                verify(authenticationService).login(any(LoginRequestDTO.class));
        }

        @Test
        void loginUser_UserStatusInvalid() throws Exception {
                // Arrange
                LoginRequestDTO loginRequest = new LoginRequestDTO();
                loginRequest.setEmail("inactive@example.com");
                loginRequest.setPassword("Password123!");

                when(authenticationService.login(any(LoginRequestDTO.class)))
                                .thenThrow(new UserStatusNotValidException("User account is not active"));

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(loginRequest)));

                // Assert
                resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").value("User account is not active"))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

                verify(authenticationService).login(any(LoginRequestDTO.class));
        }

        @Test
        void refreshToken_Success() throws Exception {
                // Arrange
                TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
                refreshRequest.setRefreshToken("valid-refresh-token");

                TokenResponseDTO tokenResponse = new TokenResponseDTO();
                tokenResponse.setAccessToken("new-access-token");
                tokenResponse.setRefreshToken("new-refresh-token");
                tokenResponse.setAccessTokenExpiresAt(new Date().getTime() + 3600000);

                when(authenticationService.refreshToken(any(TokenRefreshRequestDTO.class))).thenReturn(tokenResponse);

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/refresh-token").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(refreshRequest)));

                // Assert
                resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.httpStatus").value("OK"))
                                .andExpect(jsonPath("$.response.accessToken").value("new-access-token"))
                                .andExpect(jsonPath("$.response.refreshToken").value("new-refresh-token"));

                verify(authenticationService).refreshToken(any(TokenRefreshRequestDTO.class));
        }

        @Test
        void refreshToken_InvalidToken() throws Exception {
                // Arrange
                TokenRefreshRequestDTO refreshRequest = new TokenRefreshRequestDTO();
                refreshRequest.setRefreshToken("invalid-refresh-token");

                when(authenticationService.refreshToken(any(TokenRefreshRequestDTO.class)))
                                .thenThrow(new RuntimeException("Invalid or expired refresh token"));

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/refresh-token").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(refreshRequest)));

                // Assert
                resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

                verify(authenticationService).refreshToken(any(TokenRefreshRequestDTO.class));
        }

        @Test
        void logout_Success() throws Exception {
                // Arrange
                TokenInvalidateRequestDTO logoutRequest = new TokenInvalidateRequestDTO();
                logoutRequest.setAccessToken("valid-access-token");
                logoutRequest.setRefreshToken("valid-refresh-token");

                doNothing().when(authenticationService).logout(any(TokenInvalidateRequestDTO.class));

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(logoutRequest)));

                // Assert
                resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
                                .andExpect(jsonPath("$.httpStatus").value("OK"));

                verify(authenticationService).logout(any(TokenInvalidateRequestDTO.class));
        }

        @Test
        void logout_TokenAlreadyInvalidated() throws Exception {
                // Arrange
                TokenInvalidateRequestDTO logoutRequest = new TokenInvalidateRequestDTO();
                logoutRequest.setAccessToken("already-invalidated-token");
                logoutRequest.setRefreshToken("already-invalidated-refresh-token");

                doThrow(new TokenAlreadyInvalidatedException()).when(authenticationService)
                                .logout(any(TokenInvalidateRequestDTO.class));

                // Act
                ResultActions resultActions = mockMvc
                                .perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(logoutRequest)));

                // Assert
                resultActions.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").value("Token has already been invalidated"))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

                verify(authenticationService).logout(any(TokenInvalidateRequestDTO.class));
        }

        @Test
        void validateToken_Success() throws Exception {
                // Arrange
                String token = "valid-token";

                doReturn(false).when(tokenValidationService).verifyAndValidate(token);

                // Act
                ResultActions resultActions = mockMvc.perform(post("/auth/validate-token").param("token", token));

                // Assert
                resultActions.andExpect(status().isOk());

                verify(tokenValidationService).verifyAndValidate(token);
        }

        @Test
        void validateToken_InvalidToken() throws Exception {
                // Arrange
                String token = "invalid-token";

                doThrow(new RuntimeException("Invalid or expired token")).when(tokenValidationService)
                                .verifyAndValidate(token);

                // Act
                ResultActions resultActions = mockMvc.perform(post("/auth/validate-token").param("token", token));

                // Assert
                resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").value("Invalid or expired token"))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

                verify(tokenValidationService).verifyAndValidate(token);
        }

        @Test
        void getAuthentication_Success() throws Exception {
                // Arrange
                String token = "valid-token";
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                "test@example.com", null,
                                Collections.singletonList(new SimpleGrantedAuthority("USER")));

                when(tokenService.getAuthentication(token)).thenReturn(authenticationToken);

                // Act
                ResultActions resultActions = mockMvc.perform(get("/auth/authenticate").param("token", token));

                // Assert
                resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.principal").value("test@example.com"))
                                .andExpect(jsonPath("$.authorities[0].authority").value("USER"));

                verify(tokenService).getAuthentication(token);
        }

        @Test
        void getAuthentication_InvalidToken() throws Exception {
                // Arrange
                String token = "invalid-token";

                when(tokenService.getAuthentication(token)).thenThrow(new RuntimeException("Invalid or expired token"));

                // Act
                ResultActions resultActions = mockMvc.perform(get("/auth/authenticate").param("token", token));

                // Assert
                resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
                                .andExpect(jsonPath("$.message").value("Invalid or expired token"))
                                .andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

                verify(tokenService).getAuthentication(token);
        }
}