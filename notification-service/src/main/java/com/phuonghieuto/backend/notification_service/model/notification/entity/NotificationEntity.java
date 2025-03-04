package com.phuonghieuto.backend.notification_service.model.notification.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.phuonghieuto.backend.notification_service.model.notification.enums.NotificationType;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String userId;
    
    private String title;
    
    @Column(length = 1000)
    private String message;
    
    private boolean read;
    
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    
    private String referenceId;
    
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}