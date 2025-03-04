package com.phuonghieuto.backend.notification_service.model.notification.dto.request;

import java.util.Map;

import com.phuonghieuto.backend.notification_service.model.notification.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequestDTO {
    @NotBlank
    private String recipientId;
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String message;
    
    @NotNull
    private NotificationType type;
    
    private String referenceId;
    
    private Map<String, Object> additionalData;
}
