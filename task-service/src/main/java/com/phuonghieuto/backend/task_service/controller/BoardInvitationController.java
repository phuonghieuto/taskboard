package com.phuonghieuto.backend.task_service.controller;

import com.phuonghieuto.backend.task_service.model.collaboration.dto.request.BoardInvitationRequestDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.dto.response.BoardInvitationResponseDTO;
import com.phuonghieuto.backend.task_service.model.collaboration.enums.InvitationStatus;
import com.phuonghieuto.backend.task_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.task_service.service.BoardInvitationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/board-invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Board Invitations", description = "APIs for managing board collaboration invitations")
public class BoardInvitationController {

        private final BoardInvitationService boardInvitationService;

        @Operation(summary = "Create a board invitation", description = "Sends an invitation to collaborate on a board")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitation created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoardInvitationResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input or duplicate invitation", content = @Content),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have access to the board", content = @Content) })
        @PostMapping("/board/{boardId}")
        public CustomResponse<BoardInvitationResponseDTO> createInvitation(
                        @Parameter(description = "Board ID", required = true) @PathVariable String boardId,
                        @Valid @RequestBody BoardInvitationRequestDTO invitationRequest) {
                log.info("Creating invitation for board: {}", boardId);
                BoardInvitationResponseDTO invitation = boardInvitationService.createInvitation(boardId,
                                invitationRequest);
                return CustomResponse.successOf(invitation);
        }

        @Operation(summary = "Get invitation details", description = "Retrieves details of a specific invitation")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitation found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoardInvitationResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have access to view this invitation", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content) })
        @GetMapping("/{id}")
        public CustomResponse<BoardInvitationResponseDTO> getInvitation(
                        @Parameter(description = "Invitation ID", required = true) @PathVariable String id) {
                log.info("Getting invitation: {}", id);
                BoardInvitationResponseDTO invitation = boardInvitationService.getInvitationById(id);
                return CustomResponse.successOf(invitation);
        }

        @Operation(summary = "Get pending invitations for a board", description = "Retrieves all pending invitations for a specific board")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - User doesn't have access to this board", content = @Content) })
        @GetMapping("/board/{boardId}")
        public CustomResponse<List<BoardInvitationResponseDTO>> getPendingInvitationsForBoard(
                        @Parameter(description = "Board ID", required = true) @PathVariable String boardId) {
                log.info("Getting pending invitations for board: {}", boardId);
                List<BoardInvitationResponseDTO> invitations = boardInvitationService
                                .getPendingInvitationsForBoard(boardId);
                return CustomResponse.successOf(invitations);
        }

        @Operation(summary = "Get pending invitations for current user", description = "Retrieves all pending invitations for the current user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitations retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
        @GetMapping("/my-invitations")
        public CustomResponse<List<BoardInvitationResponseDTO>> getMyPendingInvitations(Authentication authentication) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String email = jwt.getClaim("userEmail");
                log.info("Getting pending invitations for user: {}", email);
                List<BoardInvitationResponseDTO> invitations = boardInvitationService
                                .getPendingInvitationsForUser(email);
                return CustomResponse.successOf(invitations);
        }

        @Operation(summary = "Accept an invitation", description = "Accept a board invitation")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitation accepted", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoardInvitationResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Only the invitee can accept", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content) })
        @PutMapping("/{id}/accept")
        public CustomResponse<BoardInvitationResponseDTO> acceptInvitation(
                        @Parameter(description = "Invitation ID", required = true) @PathVariable String id) {
                log.info("Accepting invitation: {}", id);
                BoardInvitationResponseDTO invitation = boardInvitationService.updateInvitationStatus(id,
                                InvitationStatus.ACCEPTED);
                return CustomResponse.successOf(invitation);
        }

        @Operation(summary = "Decline an invitation", description = "Decline a board invitation")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitation declined", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoardInvitationResponseDTO.class))),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Only the invitee can decline", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content) })
        @PutMapping("/{id}/decline")
        public CustomResponse<BoardInvitationResponseDTO> declineInvitation(
                        @Parameter(description = "Invitation ID", required = true) @PathVariable String id) {
                log.info("Declining invitation: {}", id);
                BoardInvitationResponseDTO invitation = boardInvitationService.updateInvitationStatus(id,
                                InvitationStatus.DECLINED);
                return CustomResponse.successOf(invitation);
        }

        @Operation(summary = "Cancel an invitation", description = "Cancel a pending invitation (by inviter or board owner)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitation cancelled", content = @Content(mediaType = "application/json")),
                        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
                        @ApiResponse(responseCode = "403", description = "Forbidden - Only the inviter or board owner can cancel", content = @Content),
                        @ApiResponse(responseCode = "404", description = "Invitation not found", content = @Content) })
        @DeleteMapping("/{id}")
        public CustomResponse<Void> cancelInvitation(
                        @Parameter(description = "Invitation ID", required = true) @PathVariable String id) {
                log.info("Cancelling invitation: {}", id);
                boardInvitationService.cancelInvitation(id);
                return CustomResponse.SUCCESS;
        }

        @Operation(summary = "Get invitation by token", description = "Retrieves invitation details using an invitation token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitation found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = BoardInvitationResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Invalid or expired invitation token", content = @Content) })
        @GetMapping("/token/{token}")
        public CustomResponse<BoardInvitationResponseDTO> getInvitationByToken(
                        @Parameter(description = "Invitation token", required = true) @PathVariable String token) {
                log.info("Getting invitation by token: {}", token);
                BoardInvitationResponseDTO invitation = boardInvitationService.getInvitationByToken(token);
                return CustomResponse.successOf(invitation);
        }
}