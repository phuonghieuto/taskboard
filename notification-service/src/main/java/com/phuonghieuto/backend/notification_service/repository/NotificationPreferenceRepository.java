package com.phuonghieuto.backend.notification_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, String> {
    Optional<NotificationPreferenceEntity> findByUserId(String userId);
}