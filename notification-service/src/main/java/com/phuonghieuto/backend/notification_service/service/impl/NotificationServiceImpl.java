package com.phuonghieuto.backend.notification_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.repository.NotificationRepository;
import com.phuonghieuto.backend.notification_service.service.NotificationService;
import com.phuonghieuto.backend.notification_service.service.WebSocketService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;
    private final ObjectMapper objectMapper;

    public NotificationEntity createTaskDueSoonNotification(TaskNotificationDTO taskNotification) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(taskNotification);
            
            NotificationEntity notification = NotificationEntity.builder()
                    .userId(taskNotification.getRecipientId())
                    .title("Task Due Soon")
                    .message("Your task '" + taskNotification.getTaskTitle() + "' is due soon")
                    .type("TASK_DUE_SOON")
                    .referenceId(taskNotification.getTaskId())
                    .referenceType("TASK")
                    .read(false)
                    .payload(jsonPayload)
                    .build();
            
            NotificationEntity savedNotification = notificationRepository.save(notification);
            
            // Send real-time notification
            webSocketService.sendNotificationToUser(
                    taskNotification.getRecipientId(), 
                    savedNotification
            );
            
            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating task due soon notification", e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    public Page<NotificationEntity> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public List<NotificationEntity> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public NotificationEntity markAsRead(String notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public void markAllAsRead(String userId) {
        List<NotificationEntity> unreadNotifications = 
                notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }
}