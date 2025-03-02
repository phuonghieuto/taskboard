package com.phuonghieuto.backend.task_service.model.task.dto.request;

import java.util.Map;

import com.phuonghieuto.backend.task_service.model.task.enums.NotificationType;

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
    private String recipientEmail;
    
    @NotNull
    private NotificationType type;
    
    private Map<String, Object> data;
}
