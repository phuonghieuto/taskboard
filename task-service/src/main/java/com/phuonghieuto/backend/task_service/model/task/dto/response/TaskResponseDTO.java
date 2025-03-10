package com.phuonghieuto.backend.task_service.model.task.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {
    private String id;
    private String title;
    private String description;
    private int orderIndex;
    private String tableId;
    private String assignedUserId;
    private String dueDate;
    private String status;
}