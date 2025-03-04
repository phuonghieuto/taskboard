package com.phuonghieuto.backend.notification_service.model.notification.mapper;

import org.springframework.stereotype.Component;

import com.phuonghieuto.backend.notification_service.model.notification.dto.response.NotificationResponseDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

@Component
public class NotificationMapper {
    
    public NotificationResponseDTO entityToResponse(NotificationEntity entity) {
        return NotificationResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .read(entity.isRead())
                .type(entity.getType())
                .referenceId(entity.getReferenceId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
