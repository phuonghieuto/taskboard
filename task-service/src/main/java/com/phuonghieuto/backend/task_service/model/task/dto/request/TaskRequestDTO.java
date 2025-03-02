package com.phuonghieuto.backend.task_service.model.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDTO {
    @NotBlank(message = "Task title cannot be blank")
    private String title;
    
    private String description;
    
    private int orderIndex;
    
    @NotNull(message = "Table ID is required")
    private String tableId;
    
    private String assignedUserId;
}