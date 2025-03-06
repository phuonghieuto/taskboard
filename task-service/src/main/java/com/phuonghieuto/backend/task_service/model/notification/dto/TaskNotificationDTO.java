package com.phuonghieuto.backend.task_service.model.notification.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import com.phuonghieuto.backend.task_service.model.notification.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationDTO implements Serializable {
    private NotificationType type;
    private String taskId;
    private String taskTitle;
    private String boardId;
    private String boardName;
    private String tableId;
    private String tableName;
    private String recipientId;
    private LocalDateTime dueDate;
    private Map<String, Object> additionalData;
}
