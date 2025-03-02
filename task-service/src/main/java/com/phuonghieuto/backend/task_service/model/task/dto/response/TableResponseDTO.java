package com.phuonghieuto.backend.task_service.model.task.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableResponseDTO {
    private String id;
    private String name;
    private int orderIndex;
}