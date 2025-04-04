package com.phuonghieuto.backend.task_service.model.task.dto.response;

import lombok.*;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResponseDTO {
    private String id;
    private String name;
    private int orderIndex;
    private String boardId;
    private Set<TaskResponseDTO> tasks;
}