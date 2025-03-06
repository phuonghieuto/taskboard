package com.phuonghieuto.backend.auth_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


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
