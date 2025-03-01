package com.phuonghieuto.backend.auth_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a login request named {@link LoginRequestDTO} containing the user's email and password.
 */
@Getter
@Setter
@Builder
@ToString
public class LoginRequestDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
