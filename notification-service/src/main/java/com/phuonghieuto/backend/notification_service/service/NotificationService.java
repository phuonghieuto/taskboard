package com.phuonghieuto.backend.notification_service.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

public interface NotificationService {
    public NotificationEntity createTaskDueSoonNotification(TaskNotificationDTO taskNotification);

    public Page<NotificationEntity> getUserNotifications(String userId, Pageable pageable);

    public List<NotificationEntity> getUnreadNotifications(String userId);

    public long countUnreadNotifications(String userId);

    public NotificationEntity markAsRead(String notificationId);

    public void markAllAsRead(String userId);

    public NotificationEntity createTaskOverdueNotification(TaskNotificationDTO taskNotification);
}