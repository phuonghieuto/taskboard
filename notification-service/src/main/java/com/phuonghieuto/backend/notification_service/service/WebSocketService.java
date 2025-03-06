package com.phuonghieuto.backend.notification_service.service;

import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

public interface WebSocketService {

    public void sendNotificationToUser(String userId, NotificationEntity notification);

    public void broadcastNotification(NotificationEntity notification);

}
