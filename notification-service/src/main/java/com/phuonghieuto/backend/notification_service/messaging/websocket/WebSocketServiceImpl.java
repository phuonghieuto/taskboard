package com.phuonghieuto.backend.notification_service.messaging.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketServiceImpl implements WebSocketService {

    private final NotificationWebSocketHandler notificationWebSocketHandler;

    @Override
    public void sendNotificationToUser(String userId, NotificationEntity notification) {
        try {
            log.info("Attempting to send notification to user: {}", userId);
            notificationWebSocketHandler.sendToUser(userId, notification);
            log.info("Sent WebSocket notification to user {}: {}", userId, notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}: {}", userId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastNotification(NotificationEntity notification) {
        try {
            notificationWebSocketHandler.broadcast(notification);
            log.info("Broadcast notification to all users: {}", notification.getTitle());
        } catch (Exception e) {
            log.error("Failed to broadcast notification: {}", e.getMessage(), e);
        }
    }
}