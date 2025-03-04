package com.phuonghieuto.backend.notification_service.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.notification_service.model.notification.dto.request.EmailNotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQConsumerService {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @RabbitListener(queues = "${rabbitmq.queue.notification}")
    public void consumeNotification(NotificationRequestDTO notificationRequest) {
        log.info("Received notification from queue: {}", notificationRequest);
        notificationService.createNotification(notificationRequest);
    }

    @RabbitListener(queues = "${rabbitmq.queue.email}")
    public void consumeEmail(EmailNotificationRequestDTO emailRequest) {
        log.info("Received email request from queue: {}", emailRequest);
        emailService.sendTemplatedEmail(
            emailRequest.getRecipientEmail(),
            emailRequest.getSubject(),
            emailRequest.getTemplateName(),
            emailRequest.getTemplateVariables()
        );
    }
    
    @RabbitListener(queues = "${rabbitmq.queue.dead-letter}")
    public void processFailedMessages(Object failedMessage) {
        log.error("Handling dead letter: {}", failedMessage);
        // Implement your dead letter handling logic
        // For example, store in database for retry later or notify admins
    }
}