package com.phuonghieuto.backend.notification_service.unit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.phuonghieuto.backend.notification_service.controller.NotificationController;
import com.phuonghieuto.backend.notification_service.exception.NotificationNotFoundException;
import com.phuonghieuto.backend.notification_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.notification_service.model.notification.entity.NotificationEntity;
import com.phuonghieuto.backend.notification_service.service.NotificationService;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

        private MockMvc mockMvc;

        @Mock
        private NotificationService notificationService;

        @Mock
        private Authentication authentication;

        @Mock
        private Jwt jwt;

        @InjectMocks
        private NotificationController notificationController;

        private static final String TEST_USER_ID = "test-user-id";
        private static final String TEST_NOTIFICATION_ID = "test-notification-id";
        private NotificationEntity testNotification1;
        private NotificationEntity testNotification2;
        private List<NotificationEntity> notificationList;
        private Page<NotificationEntity> notificationPage;

        @BeforeEach
        void setUp() {
                // Set up MockMvc with exception handler
                mockMvc = MockMvcBuilders.standaloneSetup(notificationController).setControllerAdvice(new GlobalExceptionHandler())
                                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver()).build();

                // Create test notifications
                testNotification1 = createNotification("notification-1", TEST_USER_ID, "Task Due Soon",
                                "Your task 'Complete API Tests' is due soon", "TASK_DUE_SOON", "task-123", "TASK",
                                false);

                testNotification2 = createNotification("notification-2", TEST_USER_ID, "Task Overdue",
                                "Your task 'Update Documentation' is overdue by 2 days", "TASK_OVERDUE", "task-456",
                                "TASK", true);

                // Create collection of notifications
                notificationList = Arrays.asList(testNotification1, testNotification2);
                notificationPage = new PageImpl<>(notificationList, PageRequest.of(0, 10), 2);

                // Set up JWT claims

        }

        /**
         * Helper method to create test notification entities
         */
        private NotificationEntity createNotification(String id, String userId, String title, String message,
                        String type, String referenceId, String referenceType, boolean read) {

                NotificationEntity notification = new NotificationEntity();
                notification.setId(id);
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(type);
                notification.setReferenceId(referenceId);
                notification.setReferenceType(referenceType);
                notification.setRead(read);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setPayload("{\"taskId\":\"" + referenceId + "\",\"taskTitle\":\"Test Task\"}");

                return notification;
        }

        @Test
        void getUserNotifications_Success() throws Exception {
                // Arrange
                // Use ArgumentMatcher instead of a specific Pageable instance
                when(notificationService.getUserNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                                .thenReturn(notificationPage);
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Act & Assert
                mockMvc.perform(get("/notifications").principal(authentication)).andDo(print())
                                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(2)))
                                .andExpect(jsonPath("$.content[0].id").value("notification-1"))
                                .andExpect(jsonPath("$.content[0].title").value("Task Due Soon"))
                                .andExpect(jsonPath("$.content[0].read").value(false))
                                .andExpect(jsonPath("$.content[1].id").value("notification-2"))
                                .andExpect(jsonPath("$.content[1].title").value("Task Overdue"))
                                .andExpect(jsonPath("$.content[1].read").value(true));

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), any(Pageable.class));
        }

        @Test
        void getUserNotifications_EmptyList() throws Exception {
                // Arrange
                Page<NotificationEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
                when(notificationService.getUserNotifications(eq(TEST_USER_ID), any(Pageable.class)))
                                .thenReturn(emptyPage);
                                when(authentication.getPrincipal()).thenReturn(jwt);
                                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Act & Assert
                mockMvc.perform(get("/notifications").principal(authentication)).andDo(print())
                                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(0)))
                                .andExpect(jsonPath("$.totalElements").value(0));

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).getUserNotifications(eq(TEST_USER_ID), any(Pageable.class));
        }

        @Test
        void getUnreadNotifications_Success() throws Exception {
                // Arrange
                List<NotificationEntity> unreadList = Arrays.asList(testNotification1); // Only the unread notification
                when(notificationService.getUnreadNotifications(TEST_USER_ID)).thenReturn(unreadList);
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Act & Assert
                mockMvc.perform(get("/notifications/unread").principal(authentication)).andDo(print())
                                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                                .andExpect(jsonPath("$[0].id").value("notification-1"))
                                .andExpect(jsonPath("$[0].title").value("Task Due Soon"))
                                .andExpect(jsonPath("$[0].read").value(false));

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).getUnreadNotifications(TEST_USER_ID);
        }

        @Test
        void getUnreadNotifications_EmptyList() throws Exception {
                // Arrange
                when(notificationService.getUnreadNotifications(TEST_USER_ID)).thenReturn(List.of());
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Act & Assert
                mockMvc.perform(get("/notifications/unread").principal(authentication)).andDo(print())
                                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).getUnreadNotifications(TEST_USER_ID);
        }

        @Test
        void countUnreadNotifications_Success() throws Exception {
                // Arrange
                long unreadCount = 5L;
                when(notificationService.countUnreadNotifications(TEST_USER_ID)).thenReturn(unreadCount);
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Act & Assert
                mockMvc.perform(get("/notifications/unread/count").principal(authentication)).andDo(print())
                                .andExpect(status().isOk()).andExpect(content().string("5"));

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).countUnreadNotifications(TEST_USER_ID);
        }

        @Test
        void countUnreadNotifications_ZeroCount() throws Exception {
                // Arrange
                long unreadCount = 0L;
                when(notificationService.countUnreadNotifications(TEST_USER_ID)).thenReturn(unreadCount);
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Act & Assert
                mockMvc.perform(get("/notifications/unread/count").principal(authentication)).andDo(print())
                                .andExpect(status().isOk()).andExpect(content().string("0"));

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).countUnreadNotifications(TEST_USER_ID);
        }

        @Test
        void markAsRead_Success() throws Exception {
                // Create a copy of notification to represent after being marked as read
                NotificationEntity readNotification = createNotification("notification-1", TEST_USER_ID,
                                "Task Due Soon", "Your task 'Complete API Tests' is due soon", "TASK_DUE_SOON",
                                "task-123", "TASK", true); // Now marked as read

                // Arrange
                when(notificationService.markAsRead(TEST_NOTIFICATION_ID)).thenReturn(readNotification);

                // Act & Assert
                mockMvc.perform(put("/notifications/{id}/read", TEST_NOTIFICATION_ID).principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("notification-1"))
                                .andExpect(jsonPath("$.title").value("Task Due Soon"))
                                .andExpect(jsonPath("$.read").value(true)); // Verify now marked as read

                verify(notificationService, times(1)).markAsRead(TEST_NOTIFICATION_ID);
        }

        @Test
        void markAsRead_NotificationNotFound() throws Exception {
                // Arrange
                when(notificationService.markAsRead(TEST_NOTIFICATION_ID))
                                .thenThrow(new NotificationNotFoundException("Notification not found"));

                // Act & Assert
                mockMvc.perform(put("/notifications/{id}/read", TEST_NOTIFICATION_ID).principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)).andDo(print())
                                .andExpect(status().isNotFound());

                verify(notificationService, times(1)).markAsRead(TEST_NOTIFICATION_ID);
        }

        @Test
        void markAllAsRead_Success() throws Exception {
                // Arrange
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Method returns void, so nothing to mock for the return

                // Act & Assert
                mockMvc.perform(put("/notifications/read-all").principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk());

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).markAllAsRead(TEST_USER_ID);
        }

        @Test
        void markAllAsRead_ServiceError() throws Exception {
                // Arrange
                when(authentication.getPrincipal()).thenReturn(jwt);
                when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);
                // Simulate a service error
                doThrow(new RuntimeException("Error marking notifications as read")).when(notificationService)
                                .markAllAsRead(anyString());

                // Act & Assert
                mockMvc.perform(put("/notifications/read-all").principal(authentication)
                                .contentType(MediaType.APPLICATION_JSON)).andDo(print())
                                .andExpect(status().isInternalServerError());

                verify(jwt, times(1)).getClaim("userId");
                verify(notificationService, times(1)).markAllAsRead(TEST_USER_ID);
        }
}