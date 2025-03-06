package com.phuonghieuto.backend.auth_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequestDTO {

    @NotBlank
    private String refreshToken;

}
