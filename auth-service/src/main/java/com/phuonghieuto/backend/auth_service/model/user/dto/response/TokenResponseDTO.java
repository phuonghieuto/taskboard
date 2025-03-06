package com.phuonghieuto.backend.auth_service.model.user.dto.response;

import lombok.*;


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
