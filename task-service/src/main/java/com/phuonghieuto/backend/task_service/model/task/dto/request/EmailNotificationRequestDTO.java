package com.phuonghieuto.backend.task_service.model.task.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

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