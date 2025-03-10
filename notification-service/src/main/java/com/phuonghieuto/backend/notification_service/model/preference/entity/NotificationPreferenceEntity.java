package com.phuonghieuto.backend.notification_service.model.preference.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;
    
    @Builder.Default
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;
    
    @Builder.Default
    @Column(name = "websocket_enabled", nullable = false)
    private boolean websocketEnabled = true;
    
    @Builder.Default
    @Column(name = "due_soon_notifications", nullable = false)
    private boolean dueSoonNotifications = true;
    
    @Builder.Default
    @Column(name = "overdue_notifications", nullable = false)
    private boolean overdueNotifications = true;
    
    @Builder.Default
    @Column(name = "task_assignment_notifications", nullable = false)
    private boolean taskAssignmentNotifications = true;
    
    @Builder.Default
    @Column(name = "board_sharing_notifications", nullable = false)
    private boolean boardSharingNotifications = true;
    
    @Builder.Default
    @Column(name = "quiet_hours_enabled")
    private boolean quietHoursEnabled = false;
    
    @Column(name = "quiet_hours_start")
    private Integer quietHoursStart; // 0-23 hour of day
    
    @Column(name = "quiet_hours_end")
    private Integer quietHoursEnd; // 0-23 hour of day
}