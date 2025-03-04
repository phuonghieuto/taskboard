package com.phuonghieuto.backend.notification_service.service.impl;

import com.phuonghieuto.backend.notification_service.exception.NotificationNotFoundException;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.EmailNotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.response.NotificationResponseDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.model.notification.mapper.NotificationMapper;
import com.phuonghieuto.backend.notification_service.repository.NotificationRepository;
import com.phuonghieuto.backend.notification_service.service.EmailService;
import com.phuonghieuto.backend.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void createNotification(NotificationRequestDTO requestDTO) {
        NotificationEntity notification = NotificationEntity.builder().userId(requestDTO.getRecipientId())
                .title(requestDTO.getTitle()).message(requestDTO.getMessage()).type(requestDTO.getType())
                .referenceId(requestDTO.getReferenceId()).read(false).build();

        NotificationEntity savedNotification = notificationRepository.save(notification);

        // Convert to DTO for WebSocket message
        NotificationResponseDTO responseDTO = notificationMapper.entityToResponse(savedNotification);

        // Send to WebSocket
        messagingTemplate.convertAndSendToUser(requestDTO.getRecipientId(), "/queue/notifications", responseDTO);

        log.info("Created notification for user: {}", requestDTO.getRecipientId());
    }

    @Override
    public void sendEmail(EmailNotificationRequestDTO requestDTO) {
        emailService.sendTemplatedEmail(requestDTO.getRecipientEmail(), requestDTO.getSubject(),
                requestDTO.getTemplateName(), requestDTO.getTemplateVariables());
        log.info("Sent email to: {}", requestDTO.getRecipientEmail());
    }

    @Override
    public Page<NotificationResponseDTO> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::entityToResponse);
    }

    @Override
    public NotificationResponseDTO markAsRead(String notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId).orElseThrow(
                () -> new NotificationNotFoundException("Notification not found with ID: " + notificationId));

        notification.setRead(true);
        NotificationEntity savedNotification = notificationRepository.save(notification);

        return notificationMapper.entityToResponse(savedNotification);
    }

    @Override
    public void markAllAsRead(String userId) {
        notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false).forEach(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    @Override
    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }
}