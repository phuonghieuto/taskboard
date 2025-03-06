package com.phuonghieuto.backend.auth_service.model.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDTO {

    @Email(message = "Please enter valid e-mail address")
    @Size(min = 7, message = "Minimum e-mail length is 7 characters.")
    private String email;

    @Size(min = 8)
    private String password;

    @Size(min = 4, message = "Minimum username length is 4 characters.")
    @NotBlank(message = "Username can't be blank.")
    private String username;

    @NotBlank(message = "First name can't be blank.")
    private String firstName;

    @NotBlank(message = "Last name can't be blank.")
    private String lastName;

    @NotBlank(message = "Phone number can't be blank.")
    @Size(min = 10, max = 10, message = "Phone number must be 10 digits.")
    private String phoneNumber;
}
