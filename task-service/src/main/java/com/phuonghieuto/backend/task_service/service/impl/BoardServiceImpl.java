package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.BoardEntityToBoardResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.BoardRequestToBoardEntityMapper;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.service.BoardService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final BoardRequestToBoardEntityMapper boardRequestToBoardEntityMapper = BoardRequestToBoardEntityMapper
            .initialize();
    private final BoardEntityToBoardResponseMapper boardEntityToBoardResponseMapper = BoardEntityToBoardResponseMapper
            .initialize();

    @Override
    public BoardResponseDTO createBoard(BoardRequestDTO boardRequest) {
        String currentUserId = getCurrentUserId();

        BoardEntity boardEntity = boardRequestToBoardEntityMapper.mapForCreation(boardRequest, currentUserId);
        BoardEntity savedBoard = boardRepository.save(boardEntity);

        log.info("Created new board with ID: {}", savedBoard.getId());
        return boardEntityToBoardResponseMapper.map(savedBoard);
    }

    @Override
    public BoardResponseDTO getBoardById(String id) {
        String currentUserId = getCurrentUserId();
        BoardEntity boardEntity = findBoardAndCheckAccess(id, currentUserId);

        return boardEntityToBoardResponseMapper.map(boardEntity);
    }

    @Override
    @Transactional
    public List<BoardResponseDTO> getAllBoardsByUserId(String userId) {
        log.info("Fetching all boards for user: {}", userId);
        try {
            List<BoardEntity> boards = boardRepository.findByOwnerIdOrCollaboratorIdsContains(userId, userId);
            log.info("Found {} boards for user: {}", boards.size(), userId);
            List<BoardResponseDTO> response = boards.stream().map(boardEntityToBoardResponseMapper::map)
                    .collect(Collectors.toList());

            log.debug("Successfully mapped {} board entities to DTOs", response.size());

            return response;
        } catch (Exception e) {
            // Log the full exception including stack trace
            log.error("Error occurred while fetching boards for user: {}", userId, e);

            // Include the actual exception message or provide a default
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown database error";

            throw new RuntimeException("Error occurred while fetching boards for user: " + errorMessage, e);
        }
    }

    @Override
    public BoardResponseDTO updateBoard(String id, BoardRequestDTO boardRequest) {
        String currentUserId = getCurrentUserId();
        BoardEntity existingBoard = findBoardAndCheckAccess(id, currentUserId);

        // Update board properties
        existingBoard.setName(boardRequest.getName());
        if (boardRequest.getCollaboratorIds() != null) {
            existingBoard.setCollaboratorIds(boardRequest.getCollaboratorIds());
        }

        BoardEntity updatedBoard = boardRepository.save(existingBoard);
        log.info("Updated board with ID: {}", updatedBoard.getId());

        return boardEntityToBoardResponseMapper.map(updatedBoard);
    }

    @Override
    public void deleteBoard(String id) {
        String currentUserId = getCurrentUserId();
        BoardEntity boardEntity = findBoardAndCheckAccess(id, currentUserId);

        // Only owner can delete a board
        if (!boardEntity.getOwnerId().equals(currentUserId)) {
            throw new UnauthorizedAccessException("Only the owner can delete this board");
        }

        boardRepository.delete(boardEntity);
        log.info("Deleted board with ID: {}", id);
    }

    // Helper methods
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaim(TokenClaims.USER_ID.getValue());
        }
        throw new UnauthorizedAccessException("User not authenticated");
    }

    private BoardEntity findBoardAndCheckAccess(String boardId, String userId) {
        BoardEntity boardEntity = boardRepository.findById(boardId)
                .orElseThrow(() -> new BoardNotFoundException("Board not found with ID: " + boardId));

        // Check if user is owner or collaborator
        boolean hasAccess = boardEntity.getOwnerId().equals(userId)
                || (boardEntity.getCollaboratorIds() != null && boardEntity.getCollaboratorIds().contains(userId));

        if (!hasAccess) {
            throw new UnauthorizedAccessException("User does not have access to this board");
        }

        return boardEntity;
    }
}