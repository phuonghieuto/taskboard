package com.phuonghieuto.backend.task_service.model;

import java.time.LocalDateTime;

import com.phuonghieuto.backend.task_service.model.common.BaseDomainModel;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Task extends BaseDomainModel{
    private String id;
    private String title;
    private String description;
    private Table table;
    private String assignedUserId;
    private int orderIndex;
    private LocalDateTime dueDate;
    private boolean reminderSent;
    private boolean overdueNotificationSent;
    private TaskStatus status;
}
