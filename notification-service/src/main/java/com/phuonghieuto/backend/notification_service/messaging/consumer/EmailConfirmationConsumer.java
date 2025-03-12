package com.phuonghieuto.backend.notification_service.messaging.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.phuonghieuto.backend.notification_service.messaging.email.EmailService;
import com.phuonghieuto.backend.notification_service.model.notification.dto.EmailConfirmationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConfirmationConsumer {

    private final EmailService emailService;

    @RabbitListener(queues = "email.confirmation.queue")
    public void consumeEmailConfirmationMessage(EmailConfirmationDTO emailConfirmation) {
        log.info("Received email confirmation request for: {}", emailConfirmation.getEmail());
        
        try {
            emailService.sendEmailConfirmation(
                emailConfirmation.getEmail(),
                emailConfirmation.getName(),
                emailConfirmation.getToken()
            );
            
            log.info("Successfully sent confirmation email to: {}", emailConfirmation.getEmail());
        } catch (Exception e) {
            log.error("Error processing email confirmation", e);
        }
    }
}