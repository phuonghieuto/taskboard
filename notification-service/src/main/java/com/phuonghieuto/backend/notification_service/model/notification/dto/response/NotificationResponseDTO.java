package com.phuonghieuto.backend.notification_service.model.notification.dto.response;

import java.time.LocalDateTime;

import com.phuonghieuto.backend.notification_service.model.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDTO {
    private String id;
    private String userId;
    private String title;
    private String message;
    private boolean read;
    private NotificationType type;
    private String referenceId;
    private LocalDateTime createdAt;
}
