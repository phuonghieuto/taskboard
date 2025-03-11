package com.phuonghieuto.backend.notification_service.messaging.email;

import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

public interface EmailService {
    void sendNotificationEmail(NotificationEntity notification, String userEmail);
    void sendTaskDueSoonEmail(TaskNotificationDTO notification, String userEmail);
    void sendTaskOverdueEmail(TaskNotificationDTO notification, String userEmail);
    void sendBoardInvitationEmail(String recipientEmail, String inviterName, String boardName, String boardUrl);
}