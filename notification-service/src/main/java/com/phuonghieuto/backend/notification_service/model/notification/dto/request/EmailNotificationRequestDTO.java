package com.phuonghieuto.backend.notification_service.model.notification.dto.request;

import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailNotificationRequestDTO {
    @NotBlank
    @Email
    private String recipientEmail;
    
    @NotBlank
    private String subject;
    
    @NotBlank
    private String templateName;
    
    private Map<String, Object> templateVariables;
}
