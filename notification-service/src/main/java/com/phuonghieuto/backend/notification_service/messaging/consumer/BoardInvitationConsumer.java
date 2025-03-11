package com.phuonghieuto.backend.notification_service.messaging.consumer;

import com.phuonghieuto.backend.notification_service.messaging.config.RabbitMQConfig;
import com.phuonghieuto.backend.notification_service.messaging.email.EmailService;
import com.phuonghieuto.backend.notification_service.model.notification.dto.BoardInvitationNotificationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardInvitationConsumer {

    private final EmailService emailService;
    
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_TASK_NOTIFICATIONS)
    public void receiveBoardInvitation(BoardInvitationNotificationDTO invitationNotification) {
        log.info("Received board invitation notification: {}", invitationNotification);
        
        try {
            // Create the full invitation URL for the email
            String boardUrl = frontendUrl + "/invitations/" + invitationNotification.getToken();
            
            // Send invitation email
            emailService.sendBoardInvitationEmail(
                invitationNotification.getInviteeEmail(),
                invitationNotification.getInviterName(),
                invitationNotification.getBoardName(),
                boardUrl
            );
            
            log.info("Successfully sent board invitation email to: {}", invitationNotification.getInviteeEmail());
        } catch (Exception e) {
            log.error("Error processing board invitation notification", e);
        }
    }
}