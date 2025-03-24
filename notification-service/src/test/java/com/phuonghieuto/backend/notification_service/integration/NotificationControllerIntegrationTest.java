package com.phuonghieuto.backend.notification_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.notification_service.config.TestTokenConfigurationParameter;
import com.phuonghieuto.backend.notification_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.repository.NotificationRepository;
import io.jsonwebtoken.Jwts;

@AutoConfigureMockMvc
public class NotificationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestTokenConfigurationParameter tokenConfigurationParameter;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_USER_EMAIL = "test@example.com";
    private static final String OTHER_USER_ID = "other-user-id";

    private String accessToken;

    @BeforeEach
    void setUp() {
        // Clean up the database before each test
        notificationRepository.deleteAll();

        // Create test tokens
        accessToken = generateToken(TEST_USER_ID, TEST_USER_EMAIL);
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

    private NotificationEntity createNotification(String userId, String title, String message, String type,
                                                String referenceId, String referenceType, boolean read) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setRead(read);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setPayload("{\"taskId\":\"" + referenceId + "\",\"taskTitle\":\"Test Task\"}");
        return notificationRepository.save(notification);
    }

    @Test
    void getUserNotifications_Success() throws Exception {
        // Create some notifications for the test user
        createNotification(TEST_USER_ID, "Task Due Soon", "Task 1 is due soon", "TASK_DUE_SOON", "task-1", "TASK", false);
        createNotification(TEST_USER_ID, "Task Overdue", "Task 2 is overdue", "TASK_OVERDUE", "task-2", "TASK", false);
        createNotification(TEST_USER_ID, "Task Assignment", "You were assigned Task 3", "TASK_ASSIGNMENT", "task-3", "TASK", true);
        
        // Create a notification for another user (should not be returned)
        createNotification(OTHER_USER_ID, "Other User Task", "This is for another user", "TASK_DUE_SOON", "task-4", "TASK", false);

        // Get notifications for the test user
        MvcResult result = mockMvc.perform(get("/notifications")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andReturn();

        // Parse response and verify details
        String responseJson = result.getResponse().getContentAsString();
        Page<NotificationEntity> page = objectMapper.readValue(responseJson, 
                objectMapper.getTypeFactory().constructParametricType(Page.class, NotificationEntity.class));
        
        assertEquals(3, page.getTotalElements());
        assertEquals(0, page.getNumber()); // First page
    }

    @Test
    void getUnreadNotifications_Success() throws Exception {
        // Create mix of read and unread notifications
        createNotification(TEST_USER_ID, "Task Due Soon", "Task 1 is due soon", "TASK_DUE_SOON", "task-1", "TASK", false);
        createNotification(TEST_USER_ID, "Task Overdue", "Task 2 is overdue", "TASK_OVERDUE", "task-2", "TASK", false);
        createNotification(TEST_USER_ID, "Task Assignment", "You were assigned Task 3", "TASK_ASSIGNMENT", "task-3", "TASK", true);
        
        // Get unread notifications
        MvcResult result = mockMvc.perform(get("/notifications/unread")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse response and verify
        String responseJson = result.getResponse().getContentAsString();
        List<NotificationEntity> notifications = objectMapper.readValue(responseJson, 
                new TypeReference<List<NotificationEntity>>() {});
        
        assertEquals(2, notifications.size());
        assertFalse(notifications.get(0).isRead());
        assertFalse(notifications.get(1).isRead());
    }

    @Test
    void countUnreadNotifications_Success() throws Exception {
        // Create mix of read and unread notifications
        createNotification(TEST_USER_ID, "Task Due Soon", "Task 1 is due soon", "TASK_DUE_SOON", "task-1", "TASK", false);
        createNotification(TEST_USER_ID, "Task Overdue", "Task 2 is overdue", "TASK_OVERDUE", "task-2", "TASK", false);
        createNotification(TEST_USER_ID, "Task Assignment", "You were assigned Task 3", "TASK_ASSIGNMENT", "task-3", "TASK", true);
        
        // Get count of unread notifications
        MvcResult result = mockMvc.perform(get("/notifications/unread/count")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse response and verify
        String responseJson = result.getResponse().getContentAsString();
        long count = Long.parseLong(responseJson);
        
        assertEquals(2, count);
    }

    @Test
    void markAsRead_Success() throws Exception {
        // Create an unread notification
        NotificationEntity notification = createNotification(TEST_USER_ID, "Task Due Soon", "Task 1 is due soon", 
                "TASK_DUE_SOON", "task-1", "TASK", false);
        
        // Mark it as read
        MvcResult result = mockMvc.perform(put("/notifications/{id}/read", notification.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        
        // Parse response and verify it's now marked as read
        String responseJson = result.getResponse().getContentAsString();
        NotificationEntity updatedNotification = objectMapper.readValue(responseJson, NotificationEntity.class);
        
        assertTrue(updatedNotification.isRead());
        assertEquals(notification.getId(), updatedNotification.getId());
        
        // Verify in the database
        NotificationEntity dbNotification = notificationRepository.findById(notification.getId()).orElse(null);
        assertNotNull(dbNotification);
        assertTrue(dbNotification.isRead());
    }

    @Test
    void markAsRead_NotFound() throws Exception {
        // Try to mark a non-existent notification as read
        mockMvc.perform(put("/notifications/{id}/read", "non-existent-id")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void markAllAsRead_Success() throws Exception {
        // Create several unread notifications
        createNotification(TEST_USER_ID, "Task Due Soon", "Task 1 is due soon", "TASK_DUE_SOON", "task-1", "TASK", false);
        createNotification(TEST_USER_ID, "Task Overdue", "Task 2 is overdue", "TASK_OVERDUE", "task-2", "TASK", false);
        createNotification(TEST_USER_ID, "Task Assignment", "You were assigned Task 3", "TASK_ASSIGNMENT", "task-3", "TASK", false);
        
        // Create an unread notification for another user (should not be affected)
        createNotification(OTHER_USER_ID, "Other User Task", "This is for another user", "TASK_DUE_SOON", "task-4", "TASK", false);
        
        // Mark all as read for test user
        mockMvc.perform(put("/notifications/read-all")
                .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk());
        
        // Verify all test user notifications are now read
        List<NotificationEntity> testUserNotifications = notificationRepository.findByUserId(TEST_USER_ID);
        assertEquals(3, testUserNotifications.size());
        for (NotificationEntity notification : testUserNotifications) {
            assertTrue(notification.isRead(), "Notification should be marked as read");
        }
        
        // Verify other user's notifications are not affected
        List<NotificationEntity> otherUserNotifications = notificationRepository.findByUserId(OTHER_USER_ID);
        assertEquals(1, otherUserNotifications.size());
        assertFalse(otherUserNotifications.get(0).isRead(), "Other user's notification should remain unread");
    }
    
    @Test
    void getUserNotifications_Unauthorized() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void getUnreadNotifications_Unauthorized() throws Exception {
        mockMvc.perform(get("/notifications/unread"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void countUnreadNotifications_Unauthorized() throws Exception {
        mockMvc.perform(get("/notifications/unread/count"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void markAsRead_Unauthorized() throws Exception {
        mockMvc.perform(put("/notifications/{id}/read", "some-id"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void markAllAsRead_Unauthorized() throws Exception {
        mockMvc.perform(put("/notifications/read-all"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
