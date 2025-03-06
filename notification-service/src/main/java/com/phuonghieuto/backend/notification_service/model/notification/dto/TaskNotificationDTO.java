package com.phuonghieuto.backend.notification_service.model.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskNotificationDTO {
    private String type;
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