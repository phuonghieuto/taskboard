package com.phuonghieuto.backend.auth_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.auth_service.exception.UserAlreadyExistException;
import com.phuonghieuto.backend.auth_service.exception.UserNotFoundException;
import com.phuonghieuto.backend.auth_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.auth_service.model.User;
import com.phuonghieuto.backend.auth_service.model.common.CustomError;
import com.phuonghieuto.backend.auth_service.model.user.dto.request.RegisterRequestDTO;
import com.phuonghieuto.backend.auth_service.model.user.dto.response.UserEmailDTO;
import com.phuonghieuto.backend.auth_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	private MockMvc mockMvc;

	@Mock
	private UserService userService;

	@InjectMocks
	private UserController userController;

	@Mock
	private WebApplicationContext webApplicationContext;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		// Create a controller advice instance
		GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

		// Configure MockMvc with the controller and the exception handler
		mockMvc = MockMvcBuilders.standaloneSetup(userController).setControllerAdvice(exceptionHandler).build();

		objectMapper = new ObjectMapper();
	}

	@Test
	void registerUser_Success() throws Exception {
		// Arrange
		RegisterRequestDTO registerRequest = new RegisterRequestDTO();
		registerRequest.setEmail("test@example.com");
		registerRequest.setUsername("testuser");
		registerRequest.setPassword("Password123!");
		registerRequest.setFirstName("Test");
		registerRequest.setLastName("User");
		registerRequest.setPhoneNumber("1234567890");

		User user = new User();
		user.setId("user123");
		user.setEmail("test@example.com");

		when(userService.registerUser(any(RegisterRequestDTO.class))).thenReturn(user);

		// Act
		ResultActions resultActions = mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest)));

		// Assert
		resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.httpStatus").value("OK"));

		verify(userService).registerUser(any(RegisterRequestDTO.class));
	}

	@Test
	void registerUser_EmailAlreadyExists() throws Exception {
		// Arrange
		RegisterRequestDTO registerRequest = new RegisterRequestDTO();
		registerRequest.setEmail("existing@example.com");
		registerRequest.setUsername("testuser");
		registerRequest.setPassword("Password123!");
		registerRequest.setFirstName("Test");
		registerRequest.setLastName("User");
		registerRequest.setPhoneNumber("1234567890");

		when(userService.registerUser(any(RegisterRequestDTO.class)))
				.thenThrow(new UserAlreadyExistException("The email is already used for another account"));

		// Act
		ResultActions resultActions = mockMvc.perform(post("/users/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registerRequest)));

		// Assert
		resultActions.andExpect(status().isConflict()).andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.message").value("The email is already used for another account"))
				.andExpect(jsonPath("$.header").value(CustomError.Header.ALREADY_EXIST.getName()));

		verify(userService).registerUser(any(RegisterRequestDTO.class));
	}

	@Test
	void getUserEmail_Success() throws Exception {
		// Arrange
		String userId = "user123";
		UserEmailDTO userEmailDTO = UserEmailDTO.builder().userId(userId).email("test@example.com").build();

		when(userService.getUserEmail(userId)).thenReturn(userEmailDTO);

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/{userId}/email", userId));

		// Assert
		resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(userId))
				.andExpect(jsonPath("$.email").value("test@example.com"));

		verify(userService).getUserEmail(userId);
	}

	@Test
	void getUserEmail_UserNotFound() throws Exception {
		// Arrange
		String userId = "nonexistent";

		when(userService.getUserEmail(userId))
				.thenThrow(new UserNotFoundException("User not found with ID: " + userId));

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/{userId}/email", userId));

		// Assert
		resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.message").value("User not found with ID: " + userId))
				.andExpect(jsonPath("$.header").value(CustomError.Header.NOT_FOUND.getName()));

		verify(userService).getUserEmail(userId);
	}

	@Test
	void getUserIdFromEmail_Success() throws Exception {
		// Arrange
		String email = "test@example.com";
		UserEmailDTO userEmailDTO = UserEmailDTO.builder().userId("user123").email(email).build();

		when(userService.getUserIdFromEmail(email)).thenReturn(userEmailDTO);

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/by-email").param("email", email));

		// Assert
		resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.userId").value("user123"))
				.andExpect(jsonPath("$.email").value(email));

		verify(userService).getUserIdFromEmail(email);
	}

	@Test
	void getUserIdFromEmail_UserNotFound() throws Exception {
		// Arrange
		String email = "nonexistent@example.com";

		when(userService.getUserIdFromEmail(email))
				.thenThrow(new UserNotFoundException("User not found with email: " + email));

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/by-email").param("email", email));

		// Assert
		resultActions.andExpect(status().isNotFound()).andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.message").value("User not found with email: " + email))
				.andExpect(jsonPath("$.header").value(CustomError.Header.NOT_FOUND.getName()));

		verify(userService).getUserIdFromEmail(email);
	}

	@Test
	void confirmEmail_Success() throws Exception {
		// Arrange
		String token = "valid-token";

		when(userService.confirmEmail(token)).thenReturn(true);

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/confirm-email").param("token", token));

		// Assert
		resultActions.andExpect(status().isOk()).andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.httpStatus").value("OK"));

		verify(userService).confirmEmail(token);
	}

	@Test
	void confirmEmail_InvalidToken() throws Exception {
		// Arrange
		String token = "invalid-token";

		when(userService.confirmEmail(token)).thenThrow(new RuntimeException("Invalid confirmation token"));

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/confirm-email").param("token", token));

		// Assert
		resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.message").value("Invalid confirmation token"))
				.andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

		verify(userService).confirmEmail(token);
	}

	@Test
	void confirmEmail_ExpiredToken() throws Exception {
		// Arrange
		String token = "expired-token";

		when(userService.confirmEmail(token)).thenThrow(new RuntimeException("Confirmation token expired"));

		// Act
		ResultActions resultActions = mockMvc.perform(get("/users/confirm-email").param("token", token));

		// Assert
		resultActions.andExpect(status().isBadRequest()).andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.message").value("Confirmation token expired"))
				.andExpect(jsonPath("$.header").value(CustomError.Header.API_ERROR.getName()));

		verify(userService).confirmEmail(token);
	}
}