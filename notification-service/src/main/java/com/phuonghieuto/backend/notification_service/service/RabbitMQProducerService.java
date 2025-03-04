package com.phuonghieuto.backend.notification_service.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.notification_service.model.notification.dto.request.EmailNotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMQProducerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.notification}")
    private String notificationRoutingKey;

    @Value("${rabbitmq.routing-key.email}")
    private String emailRoutingKey;

    public void sendNotification(NotificationRequestDTO notificationRequest) {
        log.info("Sending notification to queue: {}", notificationRequest);
        rabbitTemplate.convertAndSend(exchange, notificationRoutingKey, notificationRequest);
    }

    public void sendEmail(EmailNotificationRequestDTO emailRequest) {
        log.info("Sending email request to queue: {}", emailRequest);
        rabbitTemplate.convertAndSend(exchange, emailRoutingKey, emailRequest);
    }
}