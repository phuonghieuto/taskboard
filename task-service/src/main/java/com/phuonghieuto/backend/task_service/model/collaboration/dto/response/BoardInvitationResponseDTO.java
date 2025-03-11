package com.phuonghieuto.backend.task_service.model.collaboration.dto.response;

import java.time.LocalDateTime;

import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardInvitationResponseDTO {
    private String id;
    private String boardId;
    private String boardName;
    private String inviterUserId;
    private String inviteeEmail;
    private String inviteeUserId;
    private String token;
    private InvitationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
