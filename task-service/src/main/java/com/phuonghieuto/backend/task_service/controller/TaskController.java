package com.phuonghieuto.backend.task_service.controller;

import com.phuonghieuto.backend.task_service.model.common.dto.response.CustomResponse;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TaskRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.enums.TaskStatus;
import com.phuonghieuto.backend.task_service.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Task Management", description = "APIs for managing tasks")
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "Create a new task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task created successfully", content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content) })
    @PostMapping
    public CustomResponse<TaskResponseDTO> createTask(@RequestBody @Valid TaskRequestDTO taskRequest) {
        log.info("TaskController | createTask");
        TaskResponseDTO response = taskService.createTask(taskRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get a task by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task found", content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content) })
    @GetMapping("/{id}")
    public CustomResponse<TaskResponseDTO> getTaskById(@PathVariable String id) {
        log.info("TaskController | getTaskById: {}", id);
        TaskResponseDTO response = taskService.getTaskById(id);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get all tasks by table ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table not found", content = @Content) })
    @GetMapping("/table/{tableId}")
    public CustomResponse<List<TaskResponseDTO>> getAllTasksByTableId(@PathVariable String tableId) {
        log.info("TaskController | getAllTasksByTableId: {}", tableId);
        List<TaskResponseDTO> tasks = taskService.getAllTasksByTableId(tableId);
        return CustomResponse.successOf(tasks);
    }

    @Operation(summary = "Get all tasks assigned to current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping("/my-tasks")
    public CustomResponse<List<TaskResponseDTO>> getMyTasks(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");

        log.info("TaskController | getMyTasks for user: {}", userId);
        List<TaskResponseDTO> tasks = taskService.getAllTasksByAssignedUserId(userId);
        return CustomResponse.successOf(tasks);
    }

    @Operation(summary = "Update a task")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task updated successfully", content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content) })
    @PutMapping("/{id}")
    public CustomResponse<TaskResponseDTO> updateTask(@PathVariable String id,
            @RequestBody @Valid TaskRequestDTO taskRequest) {
        log.info("TaskController | updateTask: {}", id);
        TaskResponseDTO response = taskService.updateTask(id, taskRequest);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Delete a task")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Task deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content) })
    @DeleteMapping("/{id}")
    public CustomResponse<Void> deleteTask(@PathVariable String id) {
        log.info("TaskController | deleteTask: {}", id);
        taskService.deleteTask(id);
        return CustomResponse.SUCCESS;
    }

    @Operation(summary = "Reorder tasks within a table")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Tasks reordered successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
            @ApiResponse(responseCode = "404", description = "Table or task not found", content = @Content) })
    @PutMapping("/table/{tableId}/reorder")
    public CustomResponse<Void> reorderTasks(@PathVariable String tableId, @RequestBody List<String> taskIds) {
        log.info("TaskController | reorderTasks for table: {}", tableId);
        taskService.reorderTasks(tableId, taskIds);
        return CustomResponse.SUCCESS;
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<TaskResponseDTO>> getUpcomingTasks() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextHour = now.plusHours(24);

        List<TaskResponseDTO> upcomingTasks = taskService.findByDueDateBetween(now, nextHour);
        return ResponseEntity.ok(upcomingTasks);
    }

    @Operation(summary = "Update task status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status updated successfully", 
                content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
            @ApiResponse(responseCode = "404", description = "Task not found", content = @Content) })
    @PatchMapping("/{id}/status")
    public CustomResponse<TaskResponseDTO> updateTaskStatus(
            @PathVariable String id, 
            @RequestBody TaskStatus status) {
        log.info("TaskController | updateTaskStatus: {} to {}", id, status);
        TaskResponseDTO response = taskService.updateTaskStatus(id, status);
        return CustomResponse.successOf(response);
    }

    @Operation(summary = "Get tasks by status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully", 
                content = @Content(schema = @Schema(implementation = List.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping("/status/{status}")
    public CustomResponse<List<TaskResponseDTO>> getTasksByStatus(
            @PathVariable TaskStatus status) {
        log.info("TaskController | getTasksByStatus: {}", status);
        List<TaskResponseDTO> tasks = taskService.getAllTasksByStatus(status);
        return CustomResponse.successOf(tasks);
    }
    
    @Operation(summary = "Get task statistics for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
    @GetMapping("/statistics")
    public ResponseEntity<Map<TaskStatus, Long>> getTaskStatistics(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userId = jwt.getClaim("userId");
        
        log.info("TaskController | getTaskStatistics for user: {}", userId);
        Map<TaskStatus, Long> statistics = taskService.getTaskStatistics(userId);
        return ResponseEntity.ok(statistics);
    }
}