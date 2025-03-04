// notification-service/src/main/java/com/phuonghieuto/backend/notification_service/controller/NotificationController.java
package com.phuonghieuto.backend.notification_service.controller;

import com.phuonghieuto.backend.notification_service.model.notification.dto.request.EmailNotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.request.NotificationRequestDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.response.NotificationResponseDTO;
import com.phuonghieuto.backend.notification_service.service.NotificationService;
import com.phuonghieuto.backend.notification_service.service.RabbitMQProducerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API", description = "Endpoints for managing notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final RabbitMQProducerService rabbitMQProducerService;

    @PostMapping
    @Operation(summary = "Create a new notification")
    public ResponseEntity<Void> createNotification(@Valid @RequestBody NotificationRequestDTO request) {
        // Now using RabbitMQ to send notification asynchronously
        rabbitMQProducerService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
    
    @PostMapping("/email")
    @Operation(summary = "Send an email notification")
    public ResponseEntity<Void> sendEmailNotification(@Valid @RequestBody EmailNotificationRequestDTO request) {
        // Now using RabbitMQ to send email asynchronously
        rabbitMQProducerService.sendEmail(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
    
    // Rest of the controller remains the same...
    @GetMapping
    @Operation(summary = "Get user notifications")
    public ResponseEntity<Page<NotificationResponseDTO>> getUserNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        String userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponseDTO> notifications = notificationService.getUserNotifications(userId, pageable);
        
        return ResponseEntity.ok(notifications);
    }
    
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable String notificationId) {
        NotificationResponseDTO notification = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(notification);
    }
    
    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllAsRead() {
        String userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        String userId = getCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        
        return ResponseEntity.ok(response);
    }
    
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaim("user_id");
        }
        throw new RuntimeException("User not authenticated");
    }
}