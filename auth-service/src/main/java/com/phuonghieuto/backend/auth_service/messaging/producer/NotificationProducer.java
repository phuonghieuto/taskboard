package com.phuonghieuto.backend.auth_service.messaging.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.phuonghieuto.backend.auth_service.model.notification.dto.EmailConfirmationDTO;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendEmailConfirmationMessage(UserEntity user) {
    EmailConfirmationDTO emailConfirmation = EmailConfirmationDTO.builder()
        .email(user.getEmail())
        .name(user.getFirstName())
        .token(user.getConfirmationToken())
        .build();
        
    log.info("Sending email confirmation message for user: {}", user.getEmail());
    rabbitTemplate.convertAndSend("notification.exchange", "email.confirmation", emailConfirmation);
}
}