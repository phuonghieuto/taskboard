package com.phuonghieuto.backend.notification_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<NotificationEntity> findByUserIdAndReadFalseOrderByCreatedAtDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
}
