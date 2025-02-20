package com.phuonghieuto.backend.user_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a request named {@link TokenInvalidateRequestDTO} to invalidate tokens.
 * This class contains the access and refresh tokens that need to be invalidated.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInvalidateRequestDTO {

    @NotBlank
    private String accessToken;

    @NotBlank
    private String refreshToken;

}
