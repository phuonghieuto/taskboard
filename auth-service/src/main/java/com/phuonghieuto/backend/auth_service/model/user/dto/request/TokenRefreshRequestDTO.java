package com.phuonghieuto.backend.auth_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a request named {@link TokenRefreshRequestDTO} to refresh an access token using a refresh token.
 * This class contains the refresh token required for obtaining a new access token.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequestDTO {

    @NotBlank
    private String refreshToken;

}
