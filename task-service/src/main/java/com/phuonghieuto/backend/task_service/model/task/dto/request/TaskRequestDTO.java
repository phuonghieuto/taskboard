package com.phuonghieuto.backend.task_service.model.task.dto.request;

import java.time.LocalDateTime;

import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRequestDTO {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotBlank(message = "Table ID is required")
    private String tableId;
    
    private String assignedUserId;
    
    private int orderIndex;
    
    private LocalDateTime dueDate;
    
    private TaskStatus status;
}