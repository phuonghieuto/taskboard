package com.phuonghieuto.backend.task_service.model.task.entity;

import java.time.LocalDateTime;

import com.phuonghieuto.backend.task_service.model.common.entity.BaseEntity;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = {"table"})
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks")
public class TaskEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private String id;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "TABLE_ID", nullable = false)
    private TableEntity table;

    @Column(name = "ASSIGNED_USER_ID")
    private String assignedUserId;

    @Column(name = "ORDER_INDEX")
    private int orderIndex;

    @Column(name = "DUE_DATE")
    private LocalDateTime dueDate;

    @Builder.Default
    @Column(name = "REMINDER_SENT")
    private boolean reminderSent = false;
    
    @Builder.Default
    @Column(name = "overdue_notification_sent")
    private boolean overdueNotificationSent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS")
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;
}
