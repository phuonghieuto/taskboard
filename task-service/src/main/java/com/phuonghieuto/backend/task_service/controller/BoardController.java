package com.phuonghieuto.backend.task_service.controller;

import com.phuonghieuto.backend.task_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.task_service.model.task.dto.request.BoardRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.BoardResponseDTO;
import com.phuonghieuto.backend.task_service.service.BoardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
@Tag(name = "Board Management", description = "APIs for managing project boards with tables and tasks")
public class BoardController {

    private final BoardService boardService;

    @Operation(
        summary = "Create a new board",
        description = "Creates a new board for the authenticated user, optionally with collaborators"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Board created successfully", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BoardResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                        "name": "Project Alpha",
                        "ownerId": "cb9977d5-6c4c-41fb-9f67-d8a0e5b98856",
                        "collaboratorIds": ["5c7d3e8f-b8a2-4f4d-9c7b-cd42d5f3721d"],
                        "tables": []
                      },
                      "timestamp": "2024-05-26T10:15:23.456Z",
                      "path": "/api/v1/boards"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid input",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 400,
                        "message": "Validation failed",
                        "details": {
                          "name": "Board name cannot be blank"
                        }
                      },
                      "timestamp": "2024-05-26T10:15:23.456Z",
                      "path": "/api/v1/boards"
                    }
                    """
                )
            )
        )
    })
    @PostMapping
    public CustomResponse<BoardResponseDTO> createBoard(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Board creation request details",
            required = true,
            content = @Content(
                schema = @Schema(implementation = BoardRequestDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "name": "Project Alpha",
                      "collaboratorIds": ["5c7d3e8f-b8a2-4f4d-9c7b-cd42d5f3721d"]
                    }
                    """
                )
            )
        )
        @RequestBody @Valid BoardRequestDTO boardRequest) {
        log.info("BoardController | createBoard");
        BoardResponseDTO response = boardService.createBoard(boardRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(
        summary = "Get a board by ID",
        description = "Retrieves a specific board with all its tables and tasks"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Board found", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BoardResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                        "name": "Project Alpha",
                        "ownerId": "cb9977d5-6c4c-41fb-9f67-d8a0e5b98856",
                        "collaboratorIds": ["5c7d3e8f-b8a2-4f4d-9c7b-cd42d5f3721d"],
                        "tables": [
                          {
                            "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                            "name": "To Do",
                            "orderIndex": 0,
                            "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                            "tasks": [
                              {
                                "id": "task123",
                                "title": "Research API options",
                                "description": "Evaluate available API frameworks",
                                "orderIndex": 0,
                                "assignedUserId": "cb9977d5-6c4c-41fb-9f67-d8a0e5b98856",
                                "tableId": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
                              }
                            ]
                          },
                          {
                            "id": "b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7",
                            "name": "In Progress",
                            "orderIndex": 1,
                            "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                            "tasks": []
                          }
                        ]
                      },
                      "timestamp": "2024-05-26T10:17:42.123Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Board not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "Board with id 7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1 not found",
                        "details": null
                      },
                      "timestamp": "2024-05-26T10:17:42.123Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/{id}")
    public CustomResponse<BoardResponseDTO> getBoardById(
        @Parameter(
            name = "id",
            description = "Board ID",
            example = "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
            required = true
        )
        @PathVariable String id) {
        log.info("BoardController | getBoardById: {}", id);
        BoardResponseDTO response = boardService.getBoardById(id);
        return CustomResponse.successOf(response);
    }

    @Operation(
        summary = "Get all boards for current user",
        description = "Retrieves all boards owned by or shared with the authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Boards retrieved successfully", 
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = BoardResponseDTO.class)),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": [
                        {
                          "id": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                          "name": "Project Alpha",
                          "ownerId": "cb9977d5-6c4c-41fb-9f67-d8a0e5b98856",
                          "collaboratorIds": ["5c7d3e8f-b8a2-4f4d-9c7b-cd42d5f3721d"],
                          "tables": []
                        },
                        {
                          "id": "8f0ab615-7c5d-4e9a-b7d8-c6e5f4d3c2b1",
                          "name": "Product Backlog",
                          "ownerId": "cb9977d5-6c4c-41fb-9f67-d8a0e5b98856",
                          "collaboratorIds": [],
                          "tables": []
                        }
                      ],
                      "timestamp": "2024-05-26T10:19:35.789Z",
                      "path": "/api/v1/boards"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        )
    })
    @GetMapping
    public CustomResponse<List<BoardResponseDTO>> getMyBoards(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        
        log.info("BoardController | getMyBoards for user: {}", userId);
        List<BoardResponseDTO> boards = boardService.getAllBoardsByUserId(userId);
        return CustomResponse.successOf(boards);
    }

    @Operation(
        summary = "Update a board",
        description = "Updates an existing board's name and collaborators"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Board updated successfully", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BoardResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                        "name": "Project Alpha (Revised)",
                        "ownerId": "cb9977d5-6c4c-41fb-9f67-d8a0e5b98856",
                        "collaboratorIds": [
                          "5c7d3e8f-b8a2-4f4d-9c7b-cd42d5f3721d",
                          "d3e5f7g9-h1i3-j5k7-l9m1-n3o5p7q9r1s3"
                        ],
                        "tables": []
                      },
                      "timestamp": "2024-05-26T10:21:14.567Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Board not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "Board with id 7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1 not found",
                        "details": null
                      },
                      "timestamp": "2024-05-26T10:21:14.567Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user is not the owner of the board",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 403,
                        "message": "You don't have permission to update this board",
                        "details": null
                      },
                      "timestamp": "2024-05-26T10:21:14.567Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        )
    })
    @PutMapping("/{id}")
    public CustomResponse<BoardResponseDTO> updateBoard(
        @Parameter(
            name = "id",
            description = "Board ID to update",
            example = "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
            required = true
        )
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Board update request details",
            required = true,
            content = @Content(
                schema = @Schema(implementation = BoardRequestDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "name": "Project Alpha (Revised)",
                      "collaboratorIds": [
                        "5c7d3e8f-b8a2-4f4d-9c7b-cd42d5f3721d",
                        "d3e5f7g9-h1i3-j5k7-l9m1-n3o5p7q9r1s3"
                      ]
                    }
                    """
                )
            )
        )
        @RequestBody @Valid BoardRequestDTO boardRequest) {
        log.info("BoardController | updateBoard: {}", id);
        BoardResponseDTO response = boardService.updateBoard(id, boardRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(
        summary = "Delete a board",
        description = "Deletes a board and all its associated tables and tasks"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Board deleted successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "timestamp": "2024-05-26T10:23:59.123Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Board not found",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "Board with id 7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1 not found",
                        "details": null
                      },
                      "timestamp": "2024-05-26T10:23:59.123Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user is not the owner of the board",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 403,
                        "message": "You don't have permission to delete this board",
                        "details": null
                      },
                      "timestamp": "2024-05-26T10:23:59.123Z",
                      "path": "/api/v1/boards/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping("/{id}")
    public CustomResponse<Void> deleteBoard(
        @Parameter(
            name = "id",
            description = "Board ID to delete",
            example = "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
            required = true
        )
        @PathVariable String id) {
        log.info("BoardController | deleteBoard: {}", id);
        boardService.deleteBoard(id);
        return CustomResponse.SUCCESS;
    }
}