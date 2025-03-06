package com.phuonghieuto.backend.notification_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.service.WebSocketService;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotificationToUser(String userId, NotificationEntity notification) {
        try {
            String destination = "/queue/notifications." + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Sent WebSocket notification to user {}: {}", userId, notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    public void broadcastNotification(NotificationEntity notification) {
        try {
            messagingTemplate.convertAndSend("/topic/global-notifications", notification);
            log.info("Broadcast notification to all users: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to broadcast notification: {}", e.getMessage(), e);
        }
    }
}