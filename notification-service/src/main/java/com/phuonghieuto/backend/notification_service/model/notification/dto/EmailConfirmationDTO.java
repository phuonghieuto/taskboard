package com.phuonghieuto.backend.notification_service.model.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailConfirmationDTO {
    private String email;
    private String name;
    private String token;
}