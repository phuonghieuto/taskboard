package com.phuonghieuto.backend.notification_service.service;

import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;

public interface NotificationPreferenceService {
    NotificationPreferenceEntity getPreferences(String userId);
    
    NotificationPreferenceEntity updatePreferences(NotificationPreferenceEntity preferences);
    
    NotificationPreferenceEntity setEmailEnabled(String userId, boolean enabled);
    
    NotificationPreferenceEntity setWebsocketEnabled(String userId, boolean enabled);
    
    NotificationPreferenceEntity configureQuietHours(String userId, boolean enabled, Integer start, Integer end);
    
    NotificationPreferenceEntity setNotificationTypeEnabled(String userId, String type, boolean enabled);
    
    NotificationPreferenceEntity resetToDefaults(String userId);
}