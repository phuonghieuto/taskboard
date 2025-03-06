// TableServiceImpl.java
package com.phuonghieuto.backend.task_service.service.impl;

import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.model.task.mapper.TableEntityToTableResponseMapper;
import com.phuonghieuto.backend.task_service.model.task.mapper.TableRequestToTableEntityMapper;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import com.phuonghieuto.backend.task_service.service.EntityAccessControlService;
import com.phuonghieuto.backend.task_service.service.TableService;
import com.phuonghieuto.backend.task_service.util.AuthUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableServiceImpl implements TableService {

    private final TableRepository tableRepository;
    private final TableRequestToTableEntityMapper tableRequestToTableEntityMapper = TableRequestToTableEntityMapper
            .initialize();
    private final TableEntityToTableResponseMapper tableEntityToTableResponseMapper = TableEntityToTableResponseMapper
            .initialize();
    private final EntityAccessControlService accessControlService;
    private final AuthUtils authUtils;

    @Override
    public TableResponseDTO createTable(TableRequestDTO tableRequest) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if board exists and user has access to it
        BoardEntity board = accessControlService.findBoardAndCheckAccess(tableRequest.getBoardId(), currentUserId);

        // Determine the order index if not specified
        if (tableRequest.getOrderIndex() <= 0) {
            List<TableEntity> existingTables = tableRepository.findByBoardIdOrderByOrderIndexAsc(board.getId());
            tableRequest.setOrderIndex(existingTables.isEmpty() ? 1 : existingTables.size() + 1);
        }

        // Create and save the table
        TableEntity tableEntity = tableRequestToTableEntityMapper.mapForCreation(tableRequest, board);
        TableEntity savedTable = tableRepository.save(tableEntity);

        log.info("Created new table with ID: {} for board: {}", savedTable.getId(), board.getId());
        return tableEntityToTableResponseMapper.map(savedTable);
    }

    @Override
    public TableResponseDTO getTableById(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        TableEntity tableEntity = accessControlService.findTableAndCheckAccess(id, currentUserId);

        return tableEntityToTableResponseMapper.map(tableEntity);
    }

    @Override
    public List<TableResponseDTO> getAllTablesByBoardId(String boardId) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if board exists and user has access to it
        accessControlService.findBoardAndCheckAccess(boardId, currentUserId);

        // Get all tables for the board, ordered by orderIndex
        List<TableEntity> tables = tableRepository.findByBoardIdOrderByOrderIndexAsc(boardId);

        return tables.stream().map(tableEntityToTableResponseMapper::map).collect(Collectors.toList());
    }

    @Override
    public TableResponseDTO updateTable(String id, TableRequestDTO tableRequest) {
        String currentUserId = authUtils.getCurrentUserId();
        TableEntity existingTable = accessControlService.findTableAndCheckAccess(id, currentUserId);

        // Update table properties
        existingTable.setName(tableRequest.getName());

        // If board ID has changed (table moved to another board)
        if (!existingTable.getBoard().getId().equals(tableRequest.getBoardId())) {
            BoardEntity newBoard = accessControlService.findBoardAndCheckAccess(tableRequest.getBoardId(),
                    currentUserId);
            existingTable.setBoard(newBoard);
        }

        // Update order index if it has changed
        if (tableRequest.getOrderIndex() > 0 && tableRequest.getOrderIndex() != existingTable.getOrderIndex()) {
            existingTable.setOrderIndex(tableRequest.getOrderIndex());
        }

        TableEntity updatedTable = tableRepository.save(existingTable);
        log.info("Updated table with ID: {}", updatedTable.getId());

        return tableEntityToTableResponseMapper.map(updatedTable);
    }

    @Override
    public void deleteTable(String id) {
        String currentUserId = authUtils.getCurrentUserId();
        TableEntity tableEntity = accessControlService.findTableAndCheckAccess(id, currentUserId);

        tableRepository.delete(tableEntity);
        log.info("Deleted table with ID: {}", id);
    }

    @Override
    @Transactional
    public void reorderTables(String boardId, List<String> tableIds) {
        String currentUserId = authUtils.getCurrentUserId();

        // Check if board exists and user has access to it
        accessControlService.findBoardAndCheckAccess(boardId, currentUserId);

        // Update order indexes based on the provided order
        IntStream.range(0, tableIds.size()).forEach(index -> {
            String tableId = tableIds.get(index);
            TableEntity table = tableRepository.findById(tableId)
                    .orElseThrow(() -> new TableNotFoundException("Table not found with ID: " + tableId));

            // Check if the table belongs to the specified board
            if (!table.getBoard().getId().equals(boardId)) {
                throw new UnauthorizedAccessException("Table does not belong to the specified board");
            }

            // Update the order index
            table.setOrderIndex(index + 1);
            tableRepository.save(table);
        });

        log.info("Reordered tables for board ID: {}", boardId);
    }
}