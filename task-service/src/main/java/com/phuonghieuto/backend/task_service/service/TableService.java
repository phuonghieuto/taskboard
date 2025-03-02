package com.phuonghieuto.backend.task_service.service;

import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;

import java.util.List;

public interface TableService {
    TableResponseDTO createTable(TableRequestDTO tableRequest);
    TableResponseDTO getTableById(String id);
    List<TableResponseDTO> getAllTablesByBoardId(String boardId);
    TableResponseDTO updateTable(String id, TableRequestDTO tableRequest);
    void deleteTable(String id);
    void reorderTables(String boardId, List<String> tableIds);
}