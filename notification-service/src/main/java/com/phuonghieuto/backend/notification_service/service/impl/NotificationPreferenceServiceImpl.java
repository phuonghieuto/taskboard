package com.phuonghieuto.backend.notification_service.service.impl;

import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;
import com.phuonghieuto.backend.notification_service.repository.NotificationPreferenceRepository;
import com.phuonghieuto.backend.notification_service.service.NotificationPreferenceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Override
    @Cacheable(value = "userPreferences", key = "#userId")
    public NotificationPreferenceEntity getPreferences(String userId) {
        log.debug("Cache miss for user preferences with userId: {}", userId);
        Optional<NotificationPreferenceEntity> existingPrefs = preferenceRepository.findByUserId(userId);
        if(existingPrefs.isPresent()) {
            return existingPrefs.get();
        }

        NotificationPreferenceEntity defaultPrefs = createDefaultPreferences(userId);
        return preferenceRepository.save(defaultPrefs);
    }

    @Override
    @CacheEvict(value = "userPreferences", key = "#preferences.userId")
    public NotificationPreferenceEntity updatePreferences(NotificationPreferenceEntity preferences) {
        // Ensure we're updating the right user's preferences
        NotificationPreferenceEntity existingPrefs = getPreferences(preferences.getUserId());

        // If an existing record exists, maintain its ID
        if (existingPrefs.getId() != null) {
            preferences.setId(existingPrefs.getId());
        }

        return preferenceRepository.save(preferences);
    }

    @Override
    @CacheEvict(value = "userPreferences", key = "#userId")
    public NotificationPreferenceEntity setEmailEnabled(String userId, boolean enabled) {
        NotificationPreferenceEntity preferences = getPreferences(userId);
        preferences.setEmailEnabled(enabled);
        return preferenceRepository.save(preferences);
    }

    @Override
    @CacheEvict(value = "userPreferences", key = "#userId")
    public NotificationPreferenceEntity setWebsocketEnabled(String userId, boolean enabled) {
        NotificationPreferenceEntity preferences = getPreferences(userId);
        preferences.setWebsocketEnabled(enabled);
        return preferenceRepository.save(preferences);
    }

    @Override
    @CacheEvict(value = "userPreferences", key = "#userId")
    public NotificationPreferenceEntity configureQuietHours(String userId, boolean enabled, Integer start,
            Integer end) {
        NotificationPreferenceEntity preferences = getPreferences(userId);
        preferences.setQuietHoursEnabled(enabled);

        // Only update start/end if they're provided
        if (start != null) {
            preferences.setQuietHoursStart(start);
        }

        if (end != null) {
            preferences.setQuietHoursEnd(end);
        }

        return preferenceRepository.save(preferences);
    }

    @Override
    @CacheEvict(value = "userPreferences", key = "#userId")
    public NotificationPreferenceEntity setNotificationTypeEnabled(String userId, String type, boolean enabled) {
        NotificationPreferenceEntity preferences = getPreferences(userId);

        // Set the appropriate field based on the notification type
        switch (type.toUpperCase()) {
        case "DUE_SOON":
            preferences.setDueSoonNotifications(enabled);
            break;
        case "OVERDUE":
            preferences.setOverdueNotifications(enabled);
            break;
        case "TASK_ASSIGNMENT":
            preferences.setTaskAssignmentNotifications(enabled);
            break;
        case "BOARD_SHARING":
            preferences.setTaskAssignmentNotifications(enabled);
            break;
        default:
            throw new IllegalArgumentException("Unknown notification type: " + type);
        }

        return preferenceRepository.save(preferences);
    }

    @Override
    @CacheEvict(value = "userPreferences", key = "#userId")
    public NotificationPreferenceEntity resetToDefaults(String userId) {
        // Get existing preferences if any
        Optional<NotificationPreferenceEntity> existingPrefs = preferenceRepository.findByUserId(userId);

        NotificationPreferenceEntity defaultPrefs = createDefaultPreferences(userId);
        existingPrefs.ifPresent(prefs -> defaultPrefs.setId(prefs.getId()));

        return preferenceRepository.save(defaultPrefs);
    }

    private NotificationPreferenceEntity createDefaultPreferences(String userId) {
        NotificationPreferenceEntity defaultPrefs = NotificationPreferenceEntity.builder().userId(userId).emailEnabled(true).websocketEnabled(true)
                .dueSoonNotifications(true).overdueNotifications(true).taskAssignmentNotifications(true)
                .boardSharingNotifications(true).quietHoursEnabled(false).build();

        return defaultPrefs;
    }
}