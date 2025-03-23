package com.phuonghieuto.backend.task_service.unit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.task_service.controller.TableController;
import com.phuonghieuto.backend.task_service.exception.BoardNotFoundException;
import com.phuonghieuto.backend.task_service.exception.TableNotFoundException;
import com.phuonghieuto.backend.task_service.exception.UnauthorizedAccessException;
import com.phuonghieuto.backend.task_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TableResponseDTO;
import com.phuonghieuto.backend.task_service.model.task.dto.response.TaskResponseDTO;
import com.phuonghieuto.backend.task_service.service.TableService;


@ExtendWith(MockitoExtension.class)
class TableControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private TableService tableService;

    @InjectMocks
    private TableController tableController;

    private static final String TEST_TABLE_ID = "test-table-id";
    private static final String TEST_BOARD_ID = "test-board-id";
    private TableRequestDTO tableRequest;
    private TableResponseDTO tableResponse;
    private List<TableResponseDTO> tableResponseList;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with exception handler
        mockMvc = MockMvcBuilders.standaloneSetup(tableController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
            
        objectMapper = new ObjectMapper();

        // Setup test data
        // Create request DTO
        tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setOrderIndex(1);
        tableRequest.setBoardId(TEST_BOARD_ID);

        // Create response DTO
        tableResponse = new TableResponseDTO();
        tableResponse.setId(TEST_TABLE_ID);
        tableResponse.setName("Test Table");
        tableResponse.setOrderIndex(1);
        tableResponse.setBoardId(TEST_BOARD_ID);
        tableResponse.setTasks(Collections.emptySet());

        // Create second table for list tests
        TableResponseDTO tableResponse2 = new TableResponseDTO();
        tableResponse2.setId("table-id-2");
        tableResponse2.setName("Second Table");
        tableResponse2.setOrderIndex(2);
        tableResponse2.setBoardId(TEST_BOARD_ID);
        tableResponse2.setTasks(Collections.emptySet());
        
        // Create list of tables
        tableResponseList = Arrays.asList(tableResponse, tableResponse2);
    }

    @Test
    void createTable_Success() throws Exception {
        when(tableService.createTable(any(TableRequestDTO.class))).thenReturn(tableResponse);

        mockMvc.perform(post("/tables")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TABLE_ID))
            .andExpect(jsonPath("$.response.name").value("Test Table"))
            .andExpect(jsonPath("$.response.orderIndex").value(1))
            .andExpect(jsonPath("$.response.boardId").value(TEST_BOARD_ID))
            .andExpect(jsonPath("$.response.tasks").isArray())
            .andExpect(jsonPath("$.response.tasks").isEmpty());

        verify(tableService, times(1)).createTable(any(TableRequestDTO.class));
    }

    @Test
    void createTable_InvalidRequest() throws Exception {
        // Create invalid request (empty name)
        TableRequestDTO invalidRequest = new TableRequestDTO();
        invalidRequest.setOrderIndex(1);
        invalidRequest.setBoardId(TEST_BOARD_ID);
        // Name is missing

        mockMvc.perform(post("/tables")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").exists());

        verify(tableService, times(0)).createTable(any(TableRequestDTO.class));
    }

    @Test
    void createTable_BoardNotFound() throws Exception {
        when(tableService.createTable(any(TableRequestDTO.class)))
            .thenThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID));

        mockMvc.perform(post("/tables")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableRequest)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

        verify(tableService, times(1)).createTable(any(TableRequestDTO.class));
    }

    @Test
    void getTableById_Success() throws Exception {
        // Add a task to the table response
        TaskResponseDTO taskResponse = new TaskResponseDTO();
        taskResponse.setId("task-id-1");
        taskResponse.setTitle("Task 1");
        taskResponse.setTableId(TEST_TABLE_ID);
        
        TableResponseDTO responseWithTask = new TableResponseDTO();
        responseWithTask.setId(TEST_TABLE_ID);
        responseWithTask.setName("Test Table");
        responseWithTask.setOrderIndex(1);
        responseWithTask.setBoardId(TEST_BOARD_ID);
        responseWithTask.setTasks(Set.of(taskResponse));

        when(tableService.getTableById(TEST_TABLE_ID)).thenReturn(responseWithTask);

        mockMvc.perform(get("/tables/{id}", TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TABLE_ID))
            .andExpect(jsonPath("$.response.name").value("Test Table"))
            .andExpect(jsonPath("$.response.tasks").isArray())
            .andExpect(jsonPath("$.response.tasks", hasSize(1)))
            .andExpect(jsonPath("$.response.tasks[0].id").value("task-id-1"));

        verify(tableService, times(1)).getTableById(TEST_TABLE_ID);
    }

    @Test
    void getTableById_NotFound() throws Exception {
        when(tableService.getTableById(TEST_TABLE_ID))
            .thenThrow(new TableNotFoundException("Table not found with ID: " + TEST_TABLE_ID));

        mockMvc.perform(get("/tables/{id}", TEST_TABLE_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Table not found with ID: " + TEST_TABLE_ID));

        verify(tableService, times(1)).getTableById(TEST_TABLE_ID);
    }

    @Test
    void getAllTablesByBoardId_Success() throws Exception {
        when(tableService.getAllTablesByBoardId(TEST_BOARD_ID)).thenReturn(tableResponseList);

        mockMvc.perform(get("/tables/board/{boardId}", TEST_BOARD_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response", hasSize(2)))
            .andExpect(jsonPath("$.response[0].id").value(TEST_TABLE_ID))
            .andExpect(jsonPath("$.response[1].id").value("table-id-2"));

        verify(tableService, times(1)).getAllTablesByBoardId(TEST_BOARD_ID);
    }

    @Test
    void getAllTablesByBoardId_EmptyList() throws Exception {
        when(tableService.getAllTablesByBoardId(TEST_BOARD_ID)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/tables/board/{boardId}", TEST_BOARD_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response").isArray())
            .andExpect(jsonPath("$.response").isEmpty());

        verify(tableService, times(1)).getAllTablesByBoardId(TEST_BOARD_ID);
    }

    @Test
    void getAllTablesByBoardId_BoardNotFound() throws Exception {
        when(tableService.getAllTablesByBoardId(TEST_BOARD_ID))
            .thenThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID));

        mockMvc.perform(get("/tables/board/{boardId}", TEST_BOARD_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

        verify(tableService, times(1)).getAllTablesByBoardId(TEST_BOARD_ID);
    }

    @Test
    void updateTable_Success() throws Exception {
        // Create an updated request
        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Updated Table Name");
        updateRequest.setOrderIndex(2);
        updateRequest.setBoardId(TEST_BOARD_ID);

        // Create expected response
        TableResponseDTO updatedResponse = new TableResponseDTO();
        updatedResponse.setId(TEST_TABLE_ID);
        updatedResponse.setName("Updated Table Name");
        updatedResponse.setOrderIndex(2);
        updatedResponse.setBoardId(TEST_BOARD_ID);
        updatedResponse.setTasks(Collections.emptySet());

        when(tableService.updateTable(eq(TEST_TABLE_ID), any(TableRequestDTO.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/tables/{id}", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.response.id").value(TEST_TABLE_ID))
            .andExpect(jsonPath("$.response.name").value("Updated Table Name"))
            .andExpect(jsonPath("$.response.orderIndex").value(2));

        verify(tableService, times(1)).updateTable(eq(TEST_TABLE_ID), any(TableRequestDTO.class));
    }

    @Test
    void updateTable_NotFound() throws Exception {
        when(tableService.updateTable(eq(TEST_TABLE_ID), any(TableRequestDTO.class)))
            .thenThrow(new TableNotFoundException("Table not found with ID: " + TEST_TABLE_ID));

        mockMvc.perform(put("/tables/{id}", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableRequest)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Table not found with ID: " + TEST_TABLE_ID));

        verify(tableService, times(1)).updateTable(eq(TEST_TABLE_ID), any(TableRequestDTO.class));
    }

    @Test
    void updateTable_Unauthorized() throws Exception {
        when(tableService.updateTable(eq(TEST_TABLE_ID), any(TableRequestDTO.class)))
            .thenThrow(new UnauthorizedAccessException("You don't have permission to update this table"));

        mockMvc.perform(put("/tables/{id}", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("You don't have permission to update this table"));

        verify(tableService, times(1)).updateTable(eq(TEST_TABLE_ID), any(TableRequestDTO.class));
    }

    @Test
    void updateTable_InvalidRequest() throws Exception {
        // Create invalid request (empty name)
        TableRequestDTO invalidRequest = new TableRequestDTO();
        invalidRequest.setOrderIndex(1);
        invalidRequest.setBoardId(TEST_BOARD_ID);
        // Name is missing

        mockMvc.perform(put("/tables/{id}", TEST_TABLE_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").exists());

        verify(tableService, times(0)).updateTable(anyString(), any(TableRequestDTO.class));
    }

    @Test
    void deleteTable_Success() throws Exception {
        doNothing().when(tableService).deleteTable(TEST_TABLE_ID);

        mockMvc.perform(delete("/tables/{id}", TEST_TABLE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true));

        verify(tableService, times(1)).deleteTable(TEST_TABLE_ID);
    }

    @Test
    void deleteTable_NotFound() throws Exception {
        doThrow(new TableNotFoundException("Table not found with ID: " + TEST_TABLE_ID))
            .when(tableService).deleteTable(TEST_TABLE_ID);

        mockMvc.perform(delete("/tables/{id}", TEST_TABLE_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Table not found with ID: " + TEST_TABLE_ID));

        verify(tableService, times(1)).deleteTable(TEST_TABLE_ID);
    }

    @Test
    void deleteTable_Unauthorized() throws Exception {
        doThrow(new UnauthorizedAccessException("You don't have permission to delete this table"))
            .when(tableService).deleteTable(TEST_TABLE_ID);

        mockMvc.perform(delete("/tables/{id}", TEST_TABLE_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("You don't have permission to delete this table"));

        verify(tableService, times(1)).deleteTable(TEST_TABLE_ID);
    }

    @Test
    void reorderTables_Success() throws Exception {
        List<String> tableIds = Arrays.asList("table-id-2", TEST_TABLE_ID);
        
        doNothing().when(tableService).reorderTables(TEST_BOARD_ID, tableIds);

        mockMvc.perform(put("/tables/board/{boardId}/reorder", TEST_BOARD_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableIds)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true));

        verify(tableService, times(1)).reorderTables(TEST_BOARD_ID, tableIds);
    }

    @Test
    void reorderTables_BoardNotFound() throws Exception {
        List<String> tableIds = Arrays.asList("table-id-2", TEST_TABLE_ID);
        
        doThrow(new BoardNotFoundException("Board not found with ID: " + TEST_BOARD_ID))
            .when(tableService).reorderTables(TEST_BOARD_ID, tableIds);

        mockMvc.perform(put("/tables/board/{boardId}/reorder", TEST_BOARD_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableIds)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("Board not found with ID: " + TEST_BOARD_ID));

        verify(tableService, times(1)).reorderTables(TEST_BOARD_ID, tableIds);
    }

    @Test
    void reorderTables_Unauthorized() throws Exception {
        List<String> tableIds = Arrays.asList("table-id-2", TEST_TABLE_ID);
        
        doThrow(new UnauthorizedAccessException("You don't have permission to reorder tables in this board"))
            .when(tableService).reorderTables(TEST_BOARD_ID, tableIds);

        mockMvc.perform(put("/tables/board/{boardId}/reorder", TEST_BOARD_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(tableIds)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("You don't have permission to reorder tables in this board"));

        verify(tableService, times(1)).reorderTables(TEST_BOARD_ID, tableIds);
    }

    @Test
    void reorderTables_EmptyList() throws Exception {
        List<String> emptyTableIds = Collections.emptyList();
        
        mockMvc.perform(put("/tables/board/{boardId}/reorder", TEST_BOARD_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(emptyTableIds)))
            .andExpect(status().isOk());

        verify(tableService, times(1)).reorderTables(TEST_BOARD_ID, emptyTableIds);
    }
}
