package com.phuonghieuto.backend.auth_service.model.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDTO {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
