package com.phuonghieuto.backend.notification_service.controller;

import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for managing user notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get user notifications", description = "Retrieves paginated notifications for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping
    public ResponseEntity<Page<NotificationEntity>> getUserNotifications(Authentication authentication,
            @Parameter(description = "Pagination parameters (page, size, sort)", example = "?page=0&size=10&sort=createdAt,desc") @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        log.info("Getting notifications for user: " + userId);
        Page<NotificationEntity> notifications = notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notifications", description = "Retrieves all unread notifications for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationEntity>> getUnreadNotifications(Authentication authentication) {

        log.info("Getting unread notifications for user");
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        List<NotificationEntity> notifications = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Count unread notifications", description = "Returns the count of unread notifications for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class), examples = @ExampleObject(value = "5"))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(Authentication authentication) {

        log.info("Counting unread notifications for user");
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        long count = notificationService.countUnreadNotifications(userId);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification marked as read successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationEntity.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content) })
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationEntity> markAsRead(
            @Parameter(description = "Notification ID", example = "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6") @PathVariable String id,
            Authentication authentication) {

        log.info("Marking notification as read: {}", id);
        NotificationEntity notification = notificationService.markAsRead(id);
        return ResponseEntity.ok(notification);
    }

    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications for the authenticated user as read")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully", content = @Content),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {

        log.info("Marking all notifications as read for user");
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}