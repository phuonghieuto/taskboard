package com.phuonghieuto.backend.notification_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.phuonghieuto.backend.notification_service.model.notification.dto.request.EmailNotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.response.NotificationResponseDTO;

public interface NotificationService {
    void createNotification(NotificationRequestDTO requestDTO);
    void sendEmail(EmailNotificationRequestDTO requestDTO);
    Page<NotificationResponseDTO> getUserNotifications(String userId, Pageable pageable);
    NotificationResponseDTO markAsRead(String notificationId);
    void markAllAsRead(String userId);
    long getUnreadCount(String userId);
}