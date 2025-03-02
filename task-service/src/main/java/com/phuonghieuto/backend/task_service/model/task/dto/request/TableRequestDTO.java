package com.phuonghieuto.backend.task_service.model.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableRequestDTO {
    
    @NotBlank(message = "Table name cannot be blank")
    private String name;
    
    private int orderIndex;
    
    @NotNull(message = "Board ID is required")
    private String boardId;
}