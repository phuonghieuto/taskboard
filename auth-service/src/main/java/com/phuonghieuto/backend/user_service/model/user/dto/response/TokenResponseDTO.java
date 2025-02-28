package com.phuonghieuto.backend.user_service.model.user.dto.response;

import lombok.*;

/**
 * Represents a response named {@link TokenResponseDTO} containing tokens for authentication.
 * This class includes the access token, its expiration time, and the refresh token.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TokenResponseDTO {

    private String accessToken;
    private Long accessTokenExpiresAt;
    private String refreshToken;

}
