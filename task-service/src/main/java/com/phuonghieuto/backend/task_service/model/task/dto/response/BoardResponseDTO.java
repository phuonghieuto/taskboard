package com.phuonghieuto.backend.task_service.model.task.dto.response;

import lombok.*;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponseDTO {
    private String id;
    private String name;
    private String ownerId;
    private Set<String> collaboratorIds;
    private Set<TableResponseDTO> tables;
}