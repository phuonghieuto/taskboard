// BoardService.java
package com.phuonghieuto.backend.task_service.service;

import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;

import java.util.List;

public interface BoardService {
    BoardResponseDTO createBoard(BoardRequestDTO boardRequest);
    BoardResponseDTO getBoardById(String id);
    List<BoardResponseDTO> getAllBoardsByUserId(String userId);
    BoardResponseDTO updateBoard(String id, BoardRequestDTO boardRequest);
    void deleteBoard(String id);
}