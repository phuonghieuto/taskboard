package com.phuonghieuto.backend.user_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a login request named {@link LoginRequestDTO} containing the user's email and password.
 */
@Getter
@Setter
@Builder
public class LoginRequestDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
