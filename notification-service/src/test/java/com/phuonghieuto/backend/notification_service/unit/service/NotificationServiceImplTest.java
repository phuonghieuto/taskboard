package com.phuonghieuto.backend.notification_service.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.notification_service.client.AuthServiceClient;
import com.phuonghieuto.backend.notification_service.messaging.email.EmailService;
import com.phuonghieuto.backend.notification_service.messaging.websocket.WebSocketService;
import com.phuonghieuto.backend.notification_service.model.auth.dto.UserEmailDTO;
import com.phuonghieuto.backend.notification_service.model.notification.dto.TaskNotificationDTO;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;
import com.phuonghieuto.backend.notification_service.repository.NotificationPreferenceRepository;
import com.phuonghieuto.backend.notification_service.repository.NotificationRepository;
import com.phuonghieuto.backend.notification_service.service.impl.NotificationServiceImpl;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final String TEST_USER_ID = "test-user-id";
    private static final String TEST_NOTIFICATION_ID = "test-notification-id";
    private static final String TEST_TASK_ID = "test-task-id";
    private static final String TEST_EMAIL = "test@example.com";

    private TaskNotificationDTO taskNotification;
    private NotificationEntity notificationEntity;
    private NotificationPreferenceEntity preferenceEntity;
    private List<NotificationEntity> notificationList;
    private String jsonPayload;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Set up task notification
        taskNotification = new TaskNotificationDTO();
        taskNotification.setTaskId(TEST_TASK_ID);
        taskNotification.setTaskTitle("Test Task");
        taskNotification.setBoardId("test-board-id");
        taskNotification.setBoardName("Test Board");
        taskNotification.setRecipientId(TEST_USER_ID);
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("daysOverdue", 2);
        taskNotification.setAdditionalData(additionalData);

        // Set up json payload
        jsonPayload = "{\"taskId\":\"test-task-id\",\"taskTitle\":\"Test Task\"}";
        // when(objectMapper.writeValueAsString(any(TaskNotificationDTO.class))).thenReturn(jsonPayload);

        // Set up notification entity
        notificationEntity = NotificationEntity.builder().id(TEST_NOTIFICATION_ID).userId(TEST_USER_ID)
                .title("Test Notification").message("Test message").type("TASK_DUE_SOON").referenceId(TEST_TASK_ID)
                .referenceType("TASK").read(false).payload(jsonPayload).createdAt(LocalDateTime.now()).build();

        // Set up preference entity with all notifications enabled
        preferenceEntity = NotificationPreferenceEntity.builder().userId(TEST_USER_ID).emailEnabled(true)
                .websocketEnabled(true).dueSoonNotifications(true).overdueNotifications(true)
                .taskAssignmentNotifications(true).boardSharingNotifications(true).quietHoursEnabled(false).build();

        // Set up notification list
        notificationList = new ArrayList<>();
        notificationList.add(notificationEntity);
    }

    @Test
    void createTaskDueSoonNotification_Success() throws Exception {
        // Arrange
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(preferenceEntity));
        when(authServiceClient.getUserEmail(TEST_USER_ID)).thenReturn(new UserEmailDTO(TEST_USER_ID, TEST_EMAIL));
        doNothing().when(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        doNothing().when(emailService).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));

        // Act
        NotificationEntity result = notificationService.createTaskDueSoonNotification(taskNotification);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_TASK_ID, result.getReferenceId());
        assertEquals("TASK", result.getReferenceType());
        assertFalse(result.isRead());

        // Verify interactions
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        verify(authServiceClient).getUserEmail(TEST_USER_ID);
        verify(emailService).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));
    }

        @Test
    void createTaskDueSoonNotification_WithQuietHours_NoNotifications() throws Exception {
        // Arrange
        preferenceEntity.setQuietHoursEnabled(true);
        preferenceEntity.setQuietHoursStart(18); // 6 PM
        preferenceEntity.setQuietHoursEnd(6);    // 6 AM
        
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(preferenceEntity));
        
        // Create spy and configure it 
        NotificationServiceImpl notificationServiceSpy = Mockito.spy(notificationService);
        Mockito.doReturn(LocalTime.of(23, 0)).when(notificationServiceSpy).getCurrentTime();
        
        // Act - Use the spy instead of the original service
        NotificationEntity result = notificationServiceSpy.createTaskDueSoonNotification(taskNotification);
        
        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());
        
        // Verify interactions - should still save notification but not send it
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(webSocketService, never()).sendNotificationToUser(anyString(), any(NotificationEntity.class));
        verify(authServiceClient, never()).getUserEmail(anyString());
        verify(emailService, never()).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), anyString());
    }

    @Test
    void createTaskDueSoonNotification_NoPreferences_CreatesDefaultAndNotifies() throws Exception {
        // Arrange
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreferenceEntity.class))).thenReturn(preferenceEntity);
        when(authServiceClient.getUserEmail(TEST_USER_ID)).thenReturn(new UserEmailDTO(TEST_USER_ID, TEST_EMAIL));
        doNothing().when(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        doNothing().when(emailService).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));

        // Act
        NotificationEntity result = notificationService.createTaskDueSoonNotification(taskNotification);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());

        // Verify interactions - should create default preferences
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(preferenceRepository).save(any(NotificationPreferenceEntity.class));
        verify(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        verify(authServiceClient).getUserEmail(TEST_USER_ID);
        verify(emailService).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));
    }

    @Test
    void createTaskDueSoonNotification_NotificationsDisabled_OnlySavesNotification() throws Exception {
        // Arrange
        preferenceEntity.setEmailEnabled(false);
        preferenceEntity.setWebsocketEnabled(false);
        preferenceEntity.setDueSoonNotifications(false);

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(preferenceEntity));

        // Act
        NotificationEntity result = notificationService.createTaskDueSoonNotification(taskNotification);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());

        // Verify interactions - should still save notification but not send it
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(webSocketService, never()).sendNotificationToUser(anyString(), any(NotificationEntity.class));
        verify(authServiceClient, never()).getUserEmail(anyString());
        verify(emailService, never()).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), anyString());
    }

    @Test
    void createTaskDueSoonNotification_EmailServiceException_ContinuesWithoutEmail() throws Exception {
        // Arrange
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(preferenceEntity));
        when(authServiceClient.getUserEmail(TEST_USER_ID)).thenReturn(new UserEmailDTO(TEST_USER_ID, TEST_EMAIL));
        doNothing().when(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));

        // Simulate email service exception
        when(authServiceClient.getUserEmail(TEST_USER_ID)).thenThrow(new RuntimeException("Email service error"));

        // Act
        NotificationEntity result = notificationService.createTaskDueSoonNotification(taskNotification);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());

        // Verify interactions - should still send websocket notification but not email
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        verify(authServiceClient).getUserEmail(TEST_USER_ID);
        verify(emailService, never()).sendTaskDueSoonEmail(any(TaskNotificationDTO.class), anyString());
    }

    @Test
    void createTaskOverdueNotification_Success() throws Exception {
        // Arrange
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(preferenceEntity));
        when(authServiceClient.getUserEmail(TEST_USER_ID)).thenReturn(new UserEmailDTO(TEST_USER_ID, TEST_EMAIL));
        doNothing().when(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        doNothing().when(emailService).sendTaskOverdueEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));

        // Act
        NotificationEntity result = notificationService.createTaskOverdueNotification(taskNotification);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_TASK_ID, result.getReferenceId());
        assertEquals("TASK", result.getReferenceType());
        assertFalse(result.isRead());

        // Verify interactions
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        verify(authServiceClient).getUserEmail(TEST_USER_ID);
        verify(emailService).sendTaskOverdueEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));
    }

    @Test
    void createTaskOverdueNotification_WithQuietHours_StillNotifies() throws Exception {
        // Arrange - Overdue notifications bypass quiet hours
        preferenceEntity.setQuietHoursEnabled(true);
        preferenceEntity.setQuietHoursStart(LocalTime.now().getHour() - 1); // Start 1 hour ago
        preferenceEntity.setQuietHoursEnd(LocalTime.now().getHour() + 1); // End 1 hour from now

        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(notificationEntity);
        when(preferenceRepository.findByUserId(TEST_USER_ID)).thenReturn(Optional.of(preferenceEntity));
        when(authServiceClient.getUserEmail(TEST_USER_ID)).thenReturn(new UserEmailDTO(TEST_USER_ID, TEST_EMAIL));
        doNothing().when(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        doNothing().when(emailService).sendTaskOverdueEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));

        // Act
        NotificationEntity result = notificationService.createTaskOverdueNotification(taskNotification);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());

        // Verify interactions - should still notify despite quiet hours (overdue is
        // urgent)
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(preferenceRepository).findByUserId(TEST_USER_ID);
        verify(webSocketService).sendNotificationToUser(eq(TEST_USER_ID), any(NotificationEntity.class));
        verify(authServiceClient).getUserEmail(TEST_USER_ID);
        verify(emailService).sendTaskOverdueEmail(any(TaskNotificationDTO.class), eq(TEST_EMAIL));
    }

    @Test
    void getUserNotifications_Success() {
        // Arrange
        Pageable pageable = Pageable.unpaged();
        Page<NotificationEntity> page = new PageImpl<>(notificationList);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID, pageable)).thenReturn(page);

        // Act
        Page<NotificationEntity> result = notificationService.getUserNotifications(TEST_USER_ID, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_NOTIFICATION_ID, result.getContent().get(0).getId());
        assertEquals(TEST_USER_ID, result.getContent().get(0).getUserId());

        // Verify repository was called
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(TEST_USER_ID, pageable);
    }

    @Test
    void getUnreadNotifications_Success() {
        // Arrange
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(TEST_USER_ID))
                .thenReturn(notificationList);

        // Act
        List<NotificationEntity> result = notificationService.getUnreadNotifications(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_NOTIFICATION_ID, result.get(0).getId());
        assertEquals(TEST_USER_ID, result.get(0).getUserId());
        assertFalse(result.get(0).isRead());

        // Verify repository was called
        verify(notificationRepository).findByUserIdAndReadFalseOrderByCreatedAtDesc(TEST_USER_ID);
    }

    @Test
    void countUnreadNotifications_Success() {
        // Arrange
        when(notificationRepository.countByUserIdAndReadFalse(TEST_USER_ID)).thenReturn(5L);

        // Act
        long result = notificationService.countUnreadNotifications(TEST_USER_ID);

        // Assert
        assertEquals(5L, result);

        // Verify repository was called
        verify(notificationRepository).countByUserIdAndReadFalse(TEST_USER_ID);
    }

    @Test
    void markAsRead_Success() {
        // Arrange
        NotificationEntity unreadNotification = NotificationEntity.builder().id(TEST_NOTIFICATION_ID)
                .userId(TEST_USER_ID).read(false).build();

        NotificationEntity readNotification = NotificationEntity.builder().id(TEST_NOTIFICATION_ID).userId(TEST_USER_ID)
                .read(true).build();

        when(notificationRepository.findById(TEST_NOTIFICATION_ID)).thenReturn(Optional.of(unreadNotification));
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(readNotification);

        // Act
        NotificationEntity result = notificationService.markAsRead(TEST_NOTIFICATION_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_NOTIFICATION_ID, result.getId());
        assertTrue(result.isRead());

        // Verify repository calls
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository).save(any(NotificationEntity.class));
    }

    @Test
    void markAsRead_NotificationNotFound() {
        // Arrange
        when(notificationRepository.findById(TEST_NOTIFICATION_ID)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(TEST_NOTIFICATION_ID);
        });
        assertEquals("Notification not found", exception.getMessage());

        // Verify repository calls
        verify(notificationRepository).findById(TEST_NOTIFICATION_ID);
        verify(notificationRepository, never()).save(any(NotificationEntity.class));
    }

    @Test
    void markAllAsRead_Success() {
        // Arrange
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(TEST_USER_ID))
                .thenReturn(notificationList);
        when(notificationRepository.saveAll(notificationList)).thenReturn(notificationList);

        // Act
        notificationService.markAllAsRead(TEST_USER_ID);

        // Assert - verify all notifications were marked as read
        assertTrue(notificationList.stream().allMatch(NotificationEntity::isRead));

        // Verify repository calls
        verify(notificationRepository).findByUserIdAndReadFalseOrderByCreatedAtDesc(TEST_USER_ID);
        verify(notificationRepository).saveAll(notificationList);
    }

    @Test
    void markAllAsRead_NoUnreadNotifications() {
        // Arrange
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(TEST_USER_ID))
                .thenReturn(new ArrayList<>());

        // Act
        notificationService.markAllAsRead(TEST_USER_ID);

        // Verify repository calls
        verify(notificationRepository).findByUserIdAndReadFalseOrderByCreatedAtDesc(TEST_USER_ID);
        verify(notificationRepository, never()).saveAll(any());
    }
}