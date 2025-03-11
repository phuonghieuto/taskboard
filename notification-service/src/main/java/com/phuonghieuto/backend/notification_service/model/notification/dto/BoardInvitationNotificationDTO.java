package com.phuonghieuto.backend.notification_service.model.notification.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardInvitationNotificationDTO {
    private String invitationId;
    private String boardId;
    private String boardName;
    private String inviterUserId;
    private String inviterName;
    private String inviteeEmail;
    private String token;
    private LocalDateTime expiresAt;
    private String invitationUrl;
}