package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.BoardEntityToBoardResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.BoardRequestToBoardEntityMapper;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.service.BoardService;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.util.AuthUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EntityAccessControlService accessControlService;
    private final AuthUtils authUtils;

    @Override
    public BoardResponseDTO createBoard(BoardRequestDTO boardRequest) {
        String currentUserId = authUtils.getCurrentUserId();

        BoardEntity boardEntity = boardRequestToBoardEntityMapper.mapForCreation(boardRequest, currentUserId);
        BoardEntity savedBoard = boardRepository.save(boardEntity);

        log.info("Created new board with ID: {}", savedBoard.getId());
        return boardEntityToBoardResponseMapper.map(savedBoard);
    }

    @Override
    public BoardResponseDTO getBoardById(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        BoardEntity boardEntity = accessControlService.findBoardAndCheckAccess(id, currentUserId);
        return boardEntityToBoardResponseMapper.map(boardEntity);
    }

    @Override
    public List<BoardResponseDTO> getAllBoardsByUserId(String userId) {
        log.info("Fetching all boards for user: {}", userId);
        try {
            List<BoardEntity> boards = boardRepository.findByOwnerIdOrCollaboratorIdsContains(userId, userId);
            log.info("Found {} boards for user: {}", boards.size(), userId);
            List<BoardResponseDTO> response = boards.stream()
                    .map(boardEntityToBoardResponseMapper::map)
                    .collect(Collectors.toList());

            log.debug("Successfully mapped {} board entities to DTOs", response.size());

            return response;
        } catch (Exception e) {
            log.error("Error occurred while fetching boards for user: {}", userId, e);
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown database error";
            throw new RuntimeException("Error occurred while fetching boards for user: " + errorMessage, e);
        }
    }

    @Override
    public BoardResponseDTO updateBoard(String id, BoardRequestDTO boardRequest) {
        String currentUserId = authUtils.getCurrentUserId();
        BoardEntity existingBoard = accessControlService.findBoardAndCheckAccess(id, currentUserId);

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
        String currentUserId = authUtils.getCurrentUserId();
        BoardEntity boardEntity = accessControlService.findBoardAndCheckAccess(id, currentUserId);

        // Only owner can delete a board
        if (!boardEntity.getOwnerId().equals(currentUserId)) {
            throw new UnauthorizedAccessException("Only the owner can delete this board");
        }

        boardRepository.delete(boardEntity);
        log.info("Deleted board with ID: {}", id);
    }
}