package com.phuonghieuto.backend.notification_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;
import com.phuonghieuto.backend.notification_service.service.NotificationPreferenceService;

@RestController
@RequestMapping("/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Preferences", description = "APIs for managing notification preferences")
public class NotificationPreferenceController {

        private final NotificationPreferenceService preferenceService;

        @Operation(summary = "Get user notification preferences")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @GetMapping
        public ResponseEntity<NotificationPreferenceEntity> getUserPreferences(Authentication authentication) {
                log.info("Getting notification preferences for user");
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                NotificationPreferenceEntity preferences = preferenceService.getPreferences(userId);
                return ResponseEntity.ok(preferences);
        }

        @Operation(summary = "Update user notification preferences")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Preferences updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @PutMapping
        public ResponseEntity<NotificationPreferenceEntity> updatePreferences(Authentication authentication,
                        @RequestBody NotificationPreferenceEntity preferences) {

                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                log.info("Updating notification preferences for user: {}", userId);

                // Safety check - ensure the userId in the JWT matches the preferences
                preferences.setUserId(userId);

                NotificationPreferenceEntity updatedPreferences = preferenceService.updatePreferences(preferences);
                return ResponseEntity.ok(updatedPreferences);
        }

        @Operation(summary = "Toggle email notifications")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Email notifications toggled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @PatchMapping("/email/{enabled}")
        public ResponseEntity<NotificationPreferenceEntity> toggleEmailNotifications(Authentication authentication,
                        @PathVariable boolean enabled) {

                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                log.info("Toggling email notifications to {} for user: {}", enabled, userId);

                NotificationPreferenceEntity updatedPreferences = preferenceService.setEmailEnabled(userId, enabled);
                return ResponseEntity.ok(updatedPreferences);
        }

        @Operation(summary = "Toggle websocket notifications")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Websocket notifications toggled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @PatchMapping("/websocket/{enabled}")
        public ResponseEntity<NotificationPreferenceEntity> toggleWebsocketNotifications(Authentication authentication,
                        @PathVariable boolean enabled) {

                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                log.info("Toggling websocket notifications to {} for user: {}", enabled, userId);

                NotificationPreferenceEntity updatedPreferences = preferenceService.setWebsocketEnabled(userId,
                                enabled);
                return ResponseEntity.ok(updatedPreferences);
        }

        @Operation(summary = "Configure quiet hours")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Quiet hours configured successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid quiet hours (must be 0-23)", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @PatchMapping("/quiet-hours")
        public ResponseEntity<NotificationPreferenceEntity> configureQuietHours(Authentication authentication,
                        @RequestParam boolean enabled, @RequestParam(required = false) Integer start,
                        @RequestParam(required = false) Integer end) {

                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                log.info("Configuring quiet hours for user: {}, enabled: {}, start: {}, end: {}", userId, enabled,
                                start, end);

                // Validate hours if enabled and provided
                if (enabled && (start != null || end != null)) {
                        if ((start != null && (start < 0 || start > 23)) || (end != null && (end < 0 || end > 23))) {
                                return ResponseEntity.badRequest().build();
                        }
                }

                NotificationPreferenceEntity updatedPreferences = preferenceService.configureQuietHours(userId, enabled,
                                start, end);
                return ResponseEntity.ok(updatedPreferences);
        }

        @Operation(summary = "Toggle notification type")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notification type toggled successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid notification type", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @PatchMapping("/type/{type}/{enabled}")
        public ResponseEntity<NotificationPreferenceEntity> toggleNotificationType(Authentication authentication,
                        @PathVariable String type, @PathVariable boolean enabled) {

                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                log.info("Toggling notification type {} to {} for user: {}", type, enabled, userId);

                try {
                        NotificationPreferenceEntity updatedPreferences = preferenceService
                                        .setNotificationTypeEnabled(userId, type, enabled);
                        return ResponseEntity.ok(updatedPreferences);
                } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().build();
                }
        }

        @Operation(summary = "Reset notification preferences to defaults")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Preferences reset successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationPreferenceEntity.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @PostMapping("/reset")
        public ResponseEntity<NotificationPreferenceEntity> resetToDefaults(Authentication authentication) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String userId = jwt.getClaim("userId");
                log.info("Resetting notification preferences to defaults for user: {}", userId);

                NotificationPreferenceEntity resetPreferences = preferenceService.resetToDefaults(userId);
                return ResponseEntity.ok(resetPreferences);
        }
}