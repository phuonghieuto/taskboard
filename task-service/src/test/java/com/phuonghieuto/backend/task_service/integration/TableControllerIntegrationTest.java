package com.phuonghieuto.backend.task_service.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.task_service.model.task.dto.request.TableRequestDTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.phuonghieuto.backend.task_service.config.TestTokenConfigurationParameter;
import com.phuonghieuto.backend.task_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.task_service.model.task.entity.BoardEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TableEntity;
import com.phuonghieuto.backend.task_service.repository.BoardRepository;
import com.phuonghieuto.backend.task_service.repository.TableRepository;
import io.jsonwebtoken.Jwts;

@AutoConfigureMockMvc
public class TableControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TableRepository tableRepository;
    
    @Autowired
    private BoardRepository boardRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private TestTokenConfigurationParameter tokenConfigurationParameter;
    
    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String OTHER_USER_ID = "other-user-id";
    // private static final String OTHER_USER_EMAIL = "other@example.com";
    
    private String accessToken;
    // private String otherUserAccessToken;
    private BoardEntity testBoard;
    
    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        tableRepository.deleteAll();
        boardRepository.deleteAll();
        
        // Create test tokens
        accessToken = generateToken(TEST_USER_ID, TEST_USER_EMAIL);
        // otherUserAccessToken = generateToken(OTHER_USER_ID, OTHER_USER_EMAIL);
        
        // Create a test board owned by the test user
        testBoard = new BoardEntity();
        testBoard.setName("Test Board");
        testBoard.setOwnerId(TEST_USER_ID);
        testBoard.setCollaboratorIds(new HashSet<>());
        testBoard = boardRepository.save(testBoard);
    }
    
    public String generateToken(String userId, String userEmail) {
        final long currentTimeMillis = System.currentTimeMillis();
        final Date tokenIssuedAt = new Date(currentTimeMillis);
        final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis), 30);

        return Jwts.builder()
                .setHeaderParam(TokenClaims.TYP.getValue(), "Bearer")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(tokenIssuedAt)
                .setExpiration(accessTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .addClaims(Map.of("userId", userId, "userEmail", userEmail))
                .compact();
    }
    
    @Test
    void createTable_Success() throws Exception {
        // Create a table request
        TableRequestDTO tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setOrderIndex(1);
        tableRequest.setBoardId(testBoard.getId());
        
        // Create the table
        mockMvc.perform(post("/tables")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tableRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.name").value("Test Table"))
                .andExpect(jsonPath("$.response.orderIndex").value(1))
                .andExpect(jsonPath("$.response.boardId").value(testBoard.getId()))
                .andExpect(jsonPath("$.response.tasks").isEmpty());
        
        // Verify the table was created in the database
        List<TableEntity> tables = tableRepository.findAll();
        assertEquals(1, tables.size());
        TableEntity savedTable = tables.get(0);
        assertEquals("Test Table", savedTable.getName());
        assertEquals(1, savedTable.getOrderIndex());
        assertEquals(testBoard.getId(), savedTable.getBoard().getId());
    }
    
    @Test
    void createTable_InvalidRequest() throws Exception {
        // Create invalid request (empty name)
        TableRequestDTO invalidRequest = new TableRequestDTO();
        invalidRequest.setOrderIndex(1);
        invalidRequest.setBoardId(testBoard.getId());
        // Name is missing
        
        mockMvc.perform(post("/tables")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
        
        // Verify no table was created
        assertEquals(0, tableRepository.findAll().size());
    }
    
    @Test
    void createTable_Unauthorized() throws Exception {
        // Try to create a table without authentication
        TableRequestDTO tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setOrderIndex(1);
        tableRequest.setBoardId(testBoard.getId());
        
        mockMvc.perform(post("/tables")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tableRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
        
        // Verify no table was created
        assertEquals(0, tableRepository.findAll().size());
    }
    
    @Test
    void createTable_BoardNotFound() throws Exception {
        // Create a table request with non-existent board ID
        TableRequestDTO tableRequest = new TableRequestDTO();
        tableRequest.setName("Test Table");
        tableRequest.setOrderIndex(1);
        tableRequest.setBoardId("non-existent-board-id");
        
        mockMvc.perform(post("/tables")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tableRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
        
        // Verify no table was created
        assertEquals(0, tableRepository.findAll().size());
    }
    
    @Test
    void getTableById_Success() throws Exception {
        // Create a table in the database
        TableEntity table = new TableEntity();
        table.setName("Test Table");
        table.setOrderIndex(1);
        table.setBoard(testBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Get the table by ID
        mockMvc.perform(get("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTable.getId()))
                .andExpect(jsonPath("$.response.name").value("Test Table"))
                .andExpect(jsonPath("$.response.orderIndex").value(1))
                .andExpect(jsonPath("$.response.boardId").value(testBoard.getId()));
    }
    
    @Test
    void getTableById_NotFound() throws Exception {
        // Try to get a non-existent table
        String nonExistentId = "non-existent-id";
        
        mockMvc.perform(get("/tables/{id}", nonExistentId)
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void getTableById_Unauthorized() throws Exception {
        // Create a table in the database
        TableEntity table = new TableEntity();
        table.setName("Test Table");
        table.setOrderIndex(1);
        table.setBoard(testBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Try to get the table without authentication
        mockMvc.perform(get("/tables/{id}", savedTable.getId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getTableById_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);
        
        // Create a table in the other user's board
        TableEntity table = new TableEntity();
        table.setName("Other User's Table");
        table.setOrderIndex(1);
        table.setBoard(otherBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Try to get the table as a different user
        mockMvc.perform(get("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void getAllTablesByBoardId_Success() throws Exception {
        // Create multiple tables in the test board
        TableEntity table1 = new TableEntity();
        table1.setName("Table 1");
        table1.setOrderIndex(1);
        table1.setBoard(testBoard);
        tableRepository.save(table1);
        
        TableEntity table2 = new TableEntity();
        table2.setName("Table 2");
        table2.setOrderIndex(2);
        table2.setBoard(testBoard);
        tableRepository.save(table2);
        
        // Get tables for the board
        mockMvc.perform(get("/tables/board/{boardId}", testBoard.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response", hasSize(2)))
                .andExpect(jsonPath("$.response[0].name").value("Table 1"))
                .andExpect(jsonPath("$.response[1].name").value("Table 2"));
    }
    
    @Test
    void getAllTablesByBoardId_EmptyList() throws Exception {
        // No tables for the test board
        mockMvc.perform(get("/tables/board/{boardId}", testBoard.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response").isArray())
                .andExpect(jsonPath("$.response", hasSize(0)));
    }
    
    @Test
    void getAllTablesByBoardId_BoardNotFound() throws Exception {
        // Try to get tables for a non-existent board
        String nonExistentBoardId = "non-existent-board-id";
        
        mockMvc.perform(get("/tables/board/{boardId}", nonExistentBoardId)
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void getAllTablesByBoardId_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);
        
        // Try to get tables for the other user's board
        mockMvc.perform(get("/tables/board/{boardId}", otherBoard.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void updateTable_Success() throws Exception {
        // Create a table in the database
        TableEntity table = new TableEntity();
        table.setName("Original Table");
        table.setOrderIndex(1);
        table.setBoard(testBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Update request
        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Updated Table");
        updateRequest.setOrderIndex(2);
        updateRequest.setBoardId(testBoard.getId());
        
        // Update the table
        mockMvc.perform(put("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTable.getId()))
                .andExpect(jsonPath("$.response.name").value("Updated Table"))
                .andExpect(jsonPath("$.response.orderIndex").value(2));
        
        // Verify the table was updated in the database
        TableEntity updatedTable = tableRepository.findById(savedTable.getId()).orElse(null);
        assertNotNull(updatedTable);
        assertEquals("Updated Table", updatedTable.getName());
        assertEquals(2, updatedTable.getOrderIndex());
    }
    
    @Test
    void updateTable_NotFound() throws Exception {
        // Try to update a non-existent table
        String nonExistentId = "non-existent-id";
        
        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Updated Table");
        updateRequest.setOrderIndex(2);
        updateRequest.setBoardId(testBoard.getId());
        
        mockMvc.perform(put("/tables/{id}", nonExistentId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void updateTable_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);
        
        // Create a table in the other user's board
        TableEntity table = new TableEntity();
        table.setName("Other User's Table");
        table.setOrderIndex(1);
        table.setBoard(otherBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Update request
        TableRequestDTO updateRequest = new TableRequestDTO();
        updateRequest.setName("Trying to Update");
        updateRequest.setOrderIndex(2);
        updateRequest.setBoardId(otherBoard.getId());
        
        // Try to update the table as a different user
        mockMvc.perform(put("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
        
        // Verify the table was not updated
        TableEntity unchangedTable = tableRepository.findById(savedTable.getId()).orElse(null);
        assertNotNull(unchangedTable);
        assertEquals("Other User's Table", unchangedTable.getName());
    }
    
    @Test
    void updateTable_InvalidRequest() throws Exception {
        // Create a table in the database
        TableEntity table = new TableEntity();
        table.setName("Original Table");
        table.setOrderIndex(1);
        table.setBoard(testBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Create invalid request (empty name)
        TableRequestDTO invalidRequest = new TableRequestDTO();
        invalidRequest.setOrderIndex(2);
        invalidRequest.setBoardId(testBoard.getId());
        // Name is missing
        
        mockMvc.perform(put("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
        
        // Verify the table was not updated
        TableEntity unchangedTable = tableRepository.findById(savedTable.getId()).orElse(null);
        assertNotNull(unchangedTable);
        assertEquals("Original Table", unchangedTable.getName());
    }
    
    @Test
    void deleteTable_Success() throws Exception {
        // Create a table in the database
        TableEntity table = new TableEntity();
        table.setName("Table to Delete");
        table.setOrderIndex(1);
        table.setBoard(testBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Delete the table
        mockMvc.perform(delete("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
        
        // Verify the table was deleted
        assertTrue(tableRepository.findById(savedTable.getId()).isEmpty());
    }
    
    @Test
    void deleteTable_NotFound() throws Exception {
        // Try to delete a non-existent table
        String nonExistentId = "non-existent-id";
        
        mockMvc.perform(delete("/tables/{id}", nonExistentId)
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void deleteTable_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);
        
        // Create a table in the other user's board
        TableEntity table = new TableEntity();
        table.setName("Other User's Table");
        table.setOrderIndex(1);
        table.setBoard(otherBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Try to delete the table as a different user
        mockMvc.perform(delete("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
        
        // Verify the table was not deleted
        assertTrue(tableRepository.findById(savedTable.getId()).isPresent());
    }
    
    @Test
    void reorderTables_Success() throws Exception {
        // Create multiple tables in the test board
        TableEntity table1 = new TableEntity();
        table1.setName("Table 1");
        table1.setOrderIndex(1);
        table1.setBoard(testBoard);
        TableEntity savedTable1 = tableRepository.save(table1);
        
        TableEntity table2 = new TableEntity();
        table2.setName("Table 2");
        table2.setOrderIndex(2);
        table2.setBoard(testBoard);
        TableEntity savedTable2 = tableRepository.save(table2);
        
        TableEntity table3 = new TableEntity();
        table3.setName("Table 3");
        table3.setOrderIndex(3);
        table3.setBoard(testBoard);
        TableEntity savedTable3 = tableRepository.save(table3);
        
        // Create reorder request (reverse the order)
        List<String> reorderedTableIds = new ArrayList<>();
        reorderedTableIds.add(savedTable3.getId());
        reorderedTableIds.add(savedTable2.getId());
        reorderedTableIds.add(savedTable1.getId());
        
        // Reorder the tables
        mockMvc.perform(put("/tables/board/{boardId}/reorder", testBoard.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reorderedTableIds)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
        
        // Verify the tables were reordered in the database
        TableEntity updatedTable1 = tableRepository.findById(savedTable1.getId()).orElse(null);
        TableEntity updatedTable2 = tableRepository.findById(savedTable2.getId()).orElse(null);
        TableEntity updatedTable3 = tableRepository.findById(savedTable3.getId()).orElse(null);
        
        assertNotNull(updatedTable1);
        assertNotNull(updatedTable2);
        assertNotNull(updatedTable3);
        
        assertEquals(3, updatedTable1.getOrderIndex()); // Was 1, now 3
        assertEquals(2, updatedTable2.getOrderIndex()); // Was 2, still 2
        assertEquals(1, updatedTable3.getOrderIndex()); // Was 3, now 1
    }
    
    @Test
    void reorderTables_BoardNotFound() throws Exception {
        // Try to reorder tables for a non-existent board
        String nonExistentBoardId = "non-existent-board-id";
        List<String> tableIds = new ArrayList<>();
        tableIds.add("table-id-1");
        
        mockMvc.perform(put("/tables/board/{boardId}/reorder", nonExistentBoardId)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tableIds)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void reorderTables_AccessDenied() throws Exception {
        // Create a board owned by OTHER_USER_ID
        BoardEntity otherBoard = new BoardEntity();
        otherBoard.setName("Other User's Board");
        otherBoard.setOwnerId(OTHER_USER_ID);
        otherBoard.setCollaboratorIds(new HashSet<>());
        otherBoard = boardRepository.save(otherBoard);
        
        // Create a table in the other user's board
        TableEntity table = new TableEntity();
        table.setName("Other User's Table");
        table.setOrderIndex(1);
        table.setBoard(otherBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // Create reorder request
        List<String> tableIds = new ArrayList<>();
        tableIds.add(savedTable.getId());
        
        // Try to reorder tables for the other user's board
        mockMvc.perform(put("/tables/board/{boardId}/reorder", otherBoard.getId())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tableIds)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    void collaboratorCanAccessTable() throws Exception {
        // Create a board with TEST_USER_ID as collaborator
        BoardEntity collaborativeBoard = new BoardEntity();
        collaborativeBoard.setName("Collaborative Board");
        collaborativeBoard.setOwnerId(OTHER_USER_ID);
        Set<String> collaboratorIds = new HashSet<>();
        collaboratorIds.add(TEST_USER_ID);
        collaborativeBoard.setCollaboratorIds(collaboratorIds);
        collaborativeBoard = boardRepository.save(collaborativeBoard);
        
        // Create a table in the collaborative board
        TableEntity table = new TableEntity();
        table.setName("Collaborative Table");
        table.setOrderIndex(1);
        table.setBoard(collaborativeBoard);
        TableEntity savedTable = tableRepository.save(table);
        
        // TEST_USER_ID should be able to access the table as a collaborator
        mockMvc.perform(get("/tables/{id}", savedTable.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.response.id").value(savedTable.getId()))
                .andExpect(jsonPath("$.response.name").value("Collaborative Table"));
    }
}
