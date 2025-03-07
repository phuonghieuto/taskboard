package com.phuonghieuto.backend.task_service.controller;

import com.phuonghieuto.backend.task_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.service.TableService;

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

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tables")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Table Management", description = "APIs for managing tables within boards")
public class TableController {

    private final TableService tableService;

    @Operation(
        summary = "Create a new table",
        description = "Creates a new table within an existing board"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Table created successfully", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TableResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                        "name": "To Do",
                        "orderIndex": 1,
                        "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                        "tasks": []
                      },
                      "timestamp": "2024-05-26T11:32:15.123Z",
                      "path": "/api/v1/tables"
                    }
                    """
                )
            )
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
                          "name": "Table name cannot be blank",
                          "boardId": "Board ID is required"
                        }
                      },
                      "timestamp": "2024-05-26T11:32:15.123Z",
                      "path": "/api/v1/tables"
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
                      "timestamp": "2024-05-26T11:32:15.123Z",
                      "path": "/api/v1/tables"
                    }
                    """
                )
            )
        )
    })
    @PostMapping
    public CustomResponse<TableResponseDTO> createTable(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Table creation details",
            required = true,
            content = @Content(
                schema = @Schema(implementation = TableRequestDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "name": "To Do",
                      "orderIndex": 1,
                      "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        )
        @RequestBody @Valid TableRequestDTO tableRequest
    ) {
        log.info("TableController | createTable");
        TableResponseDTO response = tableService.createTable(tableRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(
        summary = "Get a table by ID",
        description = "Retrieves details of a specific table including its tasks"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Table found", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TableResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                        "name": "To Do",
                        "orderIndex": 1,
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
                      "timestamp": "2024-05-26T11:33:45.456Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
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
            description = "Table not found", 
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "Table with id a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6 not found",
                        "details": null
                      },
                      "timestamp": "2024-05-26T11:33:45.456Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/{id}")
    public CustomResponse<TableResponseDTO> getTableById(
        @Parameter(
            description = "Table ID",
            example = "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
            required = true
        )
        @PathVariable String id
    ) {
        log.info("TableController | getTableById: {}", id);
        TableResponseDTO response = tableService.getTableById(id);
        return CustomResponse.successOf(response);
    }

    @Operation(
        summary = "Get all tables by board ID",
        description = "Retrieves all tables for a specific board"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tables retrieved successfully", 
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = TableResponseDTO.class)),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": [
                        {
                          "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                          "name": "To Do",
                          "orderIndex": 1,
                          "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                          "tasks": []
                        },
                        {
                          "id": "b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7",
                          "name": "In Progress",
                          "orderIndex": 2,
                          "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                          "tasks": []
                        },
                        {
                          "id": "c3d4e5f6-g7h8-i9j0-k1l2-m3n4o5p6q7r8",
                          "name": "Done",
                          "orderIndex": 3,
                          "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                          "tasks": []
                        }
                      ],
                      "timestamp": "2024-05-26T11:35:22.789Z",
                      "path": "/api/v1/tables/board/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
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
                      "timestamp": "2024-05-26T11:35:22.789Z",
                      "path": "/api/v1/tables/board/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        )
    })
    @GetMapping("/board/{boardId}")
    public CustomResponse<List<TableResponseDTO>> getAllTablesByBoardId(
        @Parameter(
            description = "Board ID",
            example = "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
            required = true
        )
        @PathVariable String boardId
    ) {
        log.info("TableController | getAllTablesByBoardId: {}", boardId);
        List<TableResponseDTO> tables = tableService.getAllTablesByBoardId(boardId);
        return CustomResponse.successOf(tables);
    }

    @Operation(
        summary = "Update a table",
        description = "Updates the name or board association of an existing table"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Table updated successfully", 
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TableResponseDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
                        "name": "Backlog",
                        "orderIndex": 1,
                        "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
                        "tasks": []
                      },
                      "timestamp": "2024-05-26T11:37:08.234Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
                    }
                    """
                )
            )
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
                          "name": "Table name cannot be blank"
                        }
                      },
                      "timestamp": "2024-05-26T11:37:08.234Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
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
            description = "Table not found", 
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "Table with id a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6 not found",
                        "details": null
                      },
                      "timestamp": "2024-05-26T11:37:08.234Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
                    }
                    """
                )
            )
        )
    })
    @PutMapping("/{id}")
    public CustomResponse<TableResponseDTO> updateTable(
        @Parameter(
            description = "Table ID",
            example = "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
            required = true
        )
        @PathVariable String id,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Table update details",
            required = true,
            content = @Content(
                schema = @Schema(implementation = TableRequestDTO.class),
                examples = @ExampleObject(
                    value = """
                    {
                      "name": "Backlog",
                      "orderIndex": 1,
                      "boardId": "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1"
                    }
                    """
                )
            )
        )
        @RequestBody @Valid TableRequestDTO tableRequest
    ) {
        log.info("TableController | updateTable: {}", id);
        TableResponseDTO response = tableService.updateTable(id, tableRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(
        summary = "Delete a table",
        description = "Deletes a table and all its associated tasks"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Table deleted successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "timestamp": "2024-05-26T11:38:45.567Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
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
            description = "Table not found", 
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": false,
                      "error": {
                        "status": 404,
                        "message": "Table with id a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6 not found",
                        "details": null
                      },
                      "timestamp": "2024-05-26T11:38:45.567Z",
                      "path": "/api/v1/tables/a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6"
                    }
                    """
                )
            )
        )
    })
    @DeleteMapping("/{id}")
    public CustomResponse<Void> deleteTable(
        @Parameter(
            description = "Table ID",
            example = "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6",
            required = true
        )
        @PathVariable String id
    ) {
        log.info("TableController | deleteTable: {}", id);
        tableService.deleteTable(id);
        return CustomResponse.SUCCESS;
    }
    
    @Operation(
        summary = "Reorder tables within a board",
        description = "Changes the order of tables in a board by updating their order indices"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Tables reordered successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    value = """
                    {
                      "success": true,
                      "timestamp": "2024-05-26T11:40:12.890Z",
                      "path": "/api/v1/tables/board/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1/reorder"
                    }
                    """
                )
            )
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
                        "message": "Table ID list must match all tables in the board",
                        "details": null
                      },
                      "timestamp": "2024-05-26T11:40:12.890Z",
                      "path": "/api/v1/tables/board/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1/reorder"
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
            description = "Board or table not found", 
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
                      "timestamp": "2024-05-26T11:40:12.890Z",
                      "path": "/api/v1/tables/board/7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1/reorder"
                    }
                    """
                )
            )
        )
    })
    @PutMapping("/board/{boardId}/reorder")
    public CustomResponse<Void> reorderTables(
        @Parameter(
            description = "Board ID",
            example = "7e9faf04-6fae-4e6c-a6fc-9d27a94bf5a1",
            required = true
        )
        @PathVariable String boardId,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "List of table IDs in the desired order (first to last)",
            required = true,
            content = @Content(
                mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(type = "string")),
                examples = @ExampleObject(
                    value = """
                    [
                      "c3d4e5f6-g7h8-i9j0-k1l2-m3n4o5p6q7r8", 
                      "a1b2c3d4-e5f6-g7h8-i9j0-k1l2m3n4o5p6", 
                      "b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7"
                    ]
                    """
                )
            )
        )
        @RequestBody List<String> tableIds
    ) {
        log.info("TableController | reorderTables for board: {}", boardId);
        tableService.reorderTables(boardId, tableIds);
        return CustomResponse.SUCCESS;
    }
}