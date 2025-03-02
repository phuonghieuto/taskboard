// BoardController.java
package com.phuonghieuto.backend.task_service.controller;

import com.phuonghieuto.backend.task_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.service.BoardService;

import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/boards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Board Management", description = "APIs for managing boards")
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "Create a new board")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Board created successfully", 
                        content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping
    public CustomResponse<BoardResponseDTO> createBoard(@RequestBody @Valid BoardRequestDTO boardRequest) {
        log.info("BoardController | createBoard");
        BoardResponseDTO response = boardService.createBoard(boardRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get a board by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Board found", 
                        content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Board not found", content = @Content)
    })
    @GetMapping("/{id}")
    public CustomResponse<BoardResponseDTO> getBoardById(@PathVariable String id) {
        log.info("BoardController | getBoardById: {}", id);
        BoardResponseDTO response = boardService.getBoardById(id);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get all boards for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Boards retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    @GetMapping
    public CustomResponse<List<BoardResponseDTO>> getMyBoards(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        
        log.info("BoardController | getMyBoards for user: {}", userId);
        List<BoardResponseDTO> boards = boardService.getAllBoardsByUserId(userId);
        return CustomResponse.successOf(boards);
    }

    @Operation(summary = "Update a board")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Board updated successfully", 
                        content = @Content(schema = @Schema(implementation = BoardResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Board not found", content = @Content)
    })
    @PutMapping("/{id}")
    public CustomResponse<BoardResponseDTO> updateBoard(
            @PathVariable String id,
            @RequestBody @Valid BoardRequestDTO boardRequest) {
        log.info("BoardController | updateBoard: {}", id);
        BoardResponseDTO response = boardService.updateBoard(id, boardRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Delete a board")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Board deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Board not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public CustomResponse<Void> deleteBoard(@PathVariable String id) {
        log.info("BoardController | deleteBoard: {}", id);
        boardService.deleteBoard(id);
        return CustomResponse.SUCCESS;
    }
}