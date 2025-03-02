package com.phuonghieuto.backend.task_service.controller;

import com.phuonghieuto.backend.task_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.service.TableService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
@Tag(name = "Table Management", description = "APIs for managing tables")
public class TableController {

    private final TableService tableService;

    @Operation(summary = "Create a new table")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table created successfully", 
                        content = @Content(schema = @Schema(implementation = TableResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping
    public CustomResponse<TableResponseDTO> createTable(@RequestBody @Valid TableRequestDTO tableRequest) {
        log.info("TableController | createTable");
        TableResponseDTO response = tableService.createTable(tableRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get a table by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table found", 
                        content = @Content(schema = @Schema(implementation = TableResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table not found", content = @Content)
    })
    @GetMapping("/{id}")
    public CustomResponse<TableResponseDTO> getTableById(@PathVariable String id) {
        log.info("TableController | getTableById: {}", id);
        TableResponseDTO response = tableService.getTableById(id);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get all tables by board ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tables retrieved successfully", 
                        content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Board not found", content = @Content)
    })
    @GetMapping("/board/{boardId}")
    public CustomResponse<List<TableResponseDTO>> getAllTablesByBoardId(@PathVariable String boardId) {
        log.info("TableController | getAllTablesByBoardId: {}", boardId);
        List<TableResponseDTO> tables = tableService.getAllTablesByBoardId(boardId);
        return CustomResponse.successOf(tables);
    }

    @Operation(summary = "Update a table")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table updated successfully", 
                        content = @Content(schema = @Schema(implementation = TableResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table not found", content = @Content)
    })
    @PutMapping("/{id}")
    public CustomResponse<TableResponseDTO> updateTable(
            @PathVariable String id,
            @RequestBody @Valid TableRequestDTO tableRequest) {
        log.info("TableController | updateTable: {}", id);
        TableResponseDTO response = tableService.updateTable(id, tableRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Delete a table")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Table deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    public CustomResponse<Void> deleteTable(@PathVariable String id) {
        log.info("TableController | deleteTable: {}", id);
        tableService.deleteTable(id);
        return CustomResponse.SUCCESS;
    }
    
    @Operation(summary = "Reorder tables within a board")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tables reordered successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "404", description = "Board or table not found", content = @Content)
    })
    @PutMapping("/board/{boardId}/reorder")
    public CustomResponse<Void> reorderTables(
            @PathVariable String boardId,
            @RequestBody List<String> tableIds) {
        log.info("TableController | reorderTables for board: {}", boardId);
        tableService.reorderTables(boardId, tableIds);
        return CustomResponse.SUCCESS;
    }
}