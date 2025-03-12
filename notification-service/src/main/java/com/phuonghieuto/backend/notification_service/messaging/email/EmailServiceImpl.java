package com.phuonghieuto.backend.notification_service.messaging.email;

import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void sendNotificationEmail(NotificationEntity notification, String userEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(notification.getTitle());
            
            // Prepare context for template
            Context context = new Context();
            context.setVariable("notification", notification);
            context.setVariable("frontendUrl", frontendUrl);
            
            // Process template
            String emailContent = templateEngine.process("email/notification-email", context);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            log.info("Email notification sent to {}", userEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendTaskDueSoonEmail(TaskNotificationDTO notification, String userEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Task Due Soon: " + notification.getTaskTitle());
            
            Context context = new Context();
            context.setVariable("taskTitle", notification.getTaskTitle());
            context.setVariable("dueDate", notification.getDueDate());
            context.setVariable("boardName", notification.getBoardName());
            context.setVariable("tableName", notification.getTableName());
            context.setVariable("taskUrl", frontendUrl + "/board/" + notification.getBoardId() + "?task=" + notification.getTaskId());
            
            String emailContent = templateEngine.process("email/task-due-soon", context);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            log.info("Task due soon email sent to {}", userEmail);
        } catch (MessagingException e) {
            log.error("Failed to send task due soon email: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendTaskOverdueEmail(TaskNotificationDTO notification, String userEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject("Task Overdue: " + notification.getTaskTitle());
            
            Context context = new Context();
            context.setVariable("taskTitle", notification.getTaskTitle());
            context.setVariable("dueDate", notification.getDueDate());
            context.setVariable("boardName", notification.getBoardName());
            context.setVariable("tableName", notification.getTableName());
            context.setVariable("taskUrl", frontendUrl + "/board/" + notification.getBoardId() + "?task=" + notification.getTaskId());
            
            String emailContent = templateEngine.process("email/task-overdue", context);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            log.info("Task overdue email sent to {}", userEmail);
        } catch (MessagingException e) {
            log.error("Failed to send task overdue email: {}", e.getMessage(), e);
        }
    }

    @Override
    public void sendBoardInvitationEmail(String recipientEmail, String inviterName, String boardName, String boardUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("You've been invited to collaborate on a board");
            
            Context context = new Context();
            context.setVariable("inviterName", inviterName);
            context.setVariable("boardName", boardName);
            context.setVariable("boardUrl", boardUrl);
            
            String emailContent = templateEngine.process("email/board-invitation", context);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            log.info("Board invitation email sent to {}", recipientEmail);
        } catch (MessagingException e) {
            log.error("Failed to send board invitation email: {}", e.getMessage(), e);
        }
    }

        @Override
    public void sendEmailConfirmation(String email, String name, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject("Confirm Your TaskManagement Account");
            
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("confirmationUrl", frontendUrl + "/confirm-email?token=" + token);
            
            String emailContent = templateEngine.process("email/email-confirmation", context);
            helper.setText(emailContent, true);
            
            mailSender.send(message);
            log.info("Email confirmation sent to {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send email confirmation: {}", e.getMessage(), e);
        }
    }
}