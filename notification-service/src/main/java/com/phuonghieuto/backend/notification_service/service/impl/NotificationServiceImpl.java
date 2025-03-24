package com.phuonghieuto.backend.notification_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.notification_service.client.AuthServiceClient;
import com.phuonghieuto.backend.notification_service.exception.NotificationNotFoundException;
import com.phuonghieuto.backend.notification_service.messaging.email.EmailService;
import com.phuonghieuto.backend.notification_service.messaging.websocket.WebSocketService;
import com.phuonghieuto.backend.notification_service.model.auth.dto.UserEmailDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;
import com.phuonghieuto.backend.notification_service.repository.NotificationPreferenceRepository;
import com.phuonghieuto.backend.notification_service.repository.NotificationRepository;
import com.phuonghieuto.backend.notification_service.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final WebSocketService webSocketService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;
    private final ObjectMapper objectMapper;

    @Override
    public NotificationEntity createTaskDueSoonNotification(TaskNotificationDTO taskNotification) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(taskNotification);

            NotificationEntity notification = NotificationEntity.builder().userId(taskNotification.getRecipientId())
                    .title("Task Due Soon").message("Your task '" + taskNotification.getTaskTitle() + "' is due soon")
                    .type("TASK_DUE_SOON").referenceId(taskNotification.getTaskId()).referenceType("TASK").read(false)
                    .payload(jsonPayload).build();

            NotificationEntity savedNotification = notificationRepository.save(notification);

            // Get user preferences
            NotificationPreferenceEntity preferences = getOrCreateUserPreferences(taskNotification.getRecipientId());

            // Check if in quiet hours
            if (preferences.isQuietHoursEnabled() && isInQuietHours(preferences)) {
                log.info("Not sending notifications during quiet hours for user: {}",
                        taskNotification.getRecipientId());
                return savedNotification;
            }

            // Send real-time WebSocket notification if enabled
            if (preferences.isWebsocketEnabled() && preferences.isDueSoonNotifications()) {
                webSocketService.sendNotificationToUser(taskNotification.getRecipientId(), savedNotification);
            }

            // Send email notification if enabled
            if (preferences.isEmailEnabled() && preferences.isDueSoonNotifications()) {
                try {
                    UserEmailDTO userEmail = authServiceClient.getUserEmail(taskNotification.getRecipientId());
                    emailService.sendTaskDueSoonEmail(taskNotification, userEmail.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send email notification for task due soon: {}", e.getMessage(), e);
                }
            }

            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating task due soon notification", e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    @Override
    public NotificationEntity createTaskOverdueNotification(TaskNotificationDTO taskNotification) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(taskNotification);

            long daysOverdue = 0;
            if (taskNotification.getAdditionalData() != null
                    && taskNotification.getAdditionalData().containsKey("daysOverdue")) {
                daysOverdue = Long.parseLong(taskNotification.getAdditionalData().get("daysOverdue").toString());
            }

            String message = "Your task '" + taskNotification.getTaskTitle() + "' is overdue";
            if (daysOverdue > 0) {
                message += " by " + daysOverdue + (daysOverdue == 1 ? " day" : " days");
            }

            NotificationEntity notification = NotificationEntity.builder().userId(taskNotification.getRecipientId())
                    .title("Task Overdue").message(message).type("TASK_OVERDUE")
                    .referenceId(taskNotification.getTaskId()).referenceType("TASK").read(false).payload(jsonPayload)
                    .build();

            NotificationEntity savedNotification = notificationRepository.save(notification);

            // Get user preferences
            NotificationPreferenceEntity preferences = getOrCreateUserPreferences(taskNotification.getRecipientId());

            // Overdue notifications bypass quiet hours for urgency

            // Send real-time WebSocket notification if enabled
            if (preferences.isWebsocketEnabled() && preferences.isOverdueNotifications()) {
                webSocketService.sendNotificationToUser(taskNotification.getRecipientId(), savedNotification);
            }

            // Send email notification if enabled
            if (preferences.isEmailEnabled() && preferences.isOverdueNotifications()) {
                try {
                    UserEmailDTO userEmail = authServiceClient.getUserEmail(taskNotification.getRecipientId());
                    emailService.sendTaskOverdueEmail(taskNotification, userEmail.getEmail());
                } catch (Exception e) {
                    log.error("Failed to send email notification for overdue task: {}", e.getMessage(), e);
                }
            }

            return savedNotification;
        } catch (Exception e) {
            log.error("Error creating task overdue notification", e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    private NotificationPreferenceEntity getOrCreateUserPreferences(String userId) {
        Optional<NotificationPreferenceEntity> existingPrefs = preferenceRepository.findByUserId(userId);

        return existingPrefs.orElseGet(() -> {
            NotificationPreferenceEntity defaultPrefs = NotificationPreferenceEntity.builder().userId(userId)
                    .emailEnabled(true).websocketEnabled(true).dueSoonNotifications(true).overdueNotifications(true)
                    .taskAssignmentNotifications(true).boardSharingNotifications(true).quietHoursEnabled(false).build();

            return preferenceRepository.save(defaultPrefs);
        });
    }

    private boolean isInQuietHours(NotificationPreferenceEntity preferences) {
        if (preferences.getQuietHoursStart() == null || preferences.getQuietHoursEnd() == null) {
            return false;
        }

        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.of(preferences.getQuietHoursStart(), 0);
        LocalTime end = LocalTime.of(preferences.getQuietHoursEnd(), 0);

        if (start.isAfter(end)) {
            // Handles overnight quiet hours (e.g., 22:00 - 07:00)
            return !now.isAfter(end) || !now.isBefore(start);
        } else {
            // Regular quiet hours (e.g., 00:00 - 07:00)
            return !now.isBefore(start) && !now.isAfter(end);
        }
    }

    // Keep your existing methods...
    @Override
    public Page<NotificationEntity> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public List<NotificationEntity> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    public long countUnreadNotifications(String userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public NotificationEntity markAsRead(String notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    public void markAllAsRead(String userId) {
        List<NotificationEntity> unreadNotifications = notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);

        if (!unreadNotifications.isEmpty()) {
            unreadNotifications.forEach(notification -> notification.setRead(true));
            notificationRepository.saveAll(unreadNotifications);
        }
    }
}