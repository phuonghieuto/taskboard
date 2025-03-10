package com.phuonghieuto.backend.auth_service.model.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEmailDTO {
    private String userId;
    private String email;
}
