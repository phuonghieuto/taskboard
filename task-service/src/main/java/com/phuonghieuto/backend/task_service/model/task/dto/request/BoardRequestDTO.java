// BoardRequestDTO.java
package com.phuonghieuto.backend.task_service.model.task.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardRequestDTO {
    
    @NotBlank(message = "Board name cannot be blank")
    private String name;
    
    private Set<String> collaboratorIds;
}