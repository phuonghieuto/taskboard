package com.phuonghieuto.backend.notification_service.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.notification_service.controller.NotificationPreferenceController;
import com.phuonghieuto.backend.notification_service.exception.exception_handler.GlobalExceptionHandler;
import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;
import com.phuonghieuto.backend.notification_service.service.NotificationPreferenceService;

/**
 * Unit tests for NotificationPreferenceController
 */
@ExtendWith(MockitoExtension.class)
class NotificationPreferenceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private Authentication authentication;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private NotificationPreferenceController preferenceController;

    private static final String TEST_USER_ID = "test-user-id";
    private NotificationPreferenceEntity testPreference;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Set up MockMvc with exception handler
        mockMvc = MockMvcBuilders.standaloneSetup(preferenceController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        // Create ObjectMapper for JSON serialization/deserialization
        objectMapper = new ObjectMapper();

        // Create test preference entity
        testPreference = NotificationPreferenceEntity.builder().id("pref-123").userId(TEST_USER_ID).emailEnabled(true)
                .websocketEnabled(true).dueSoonNotifications(true).overdueNotifications(true)
                .taskAssignmentNotifications(true).boardSharingNotifications(true).quietHoursEnabled(false)
                .quietHoursStart(22).quietHoursEnd(7).build();

        // Set up JWT claims
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaim("userId")).thenReturn(TEST_USER_ID);

    }

    @Test
    void getUserPreferences_Success() throws Exception {
        // Arrange
        when(preferenceService.getPreferences(TEST_USER_ID)).thenReturn(testPreference);

        // Act & Assert
        mockMvc.perform(get("/preferences").principal(authentication)).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pref-123")).andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.emailEnabled").value(true)).andExpect(jsonPath("$.websocketEnabled").value(true))
                .andExpect(jsonPath("$.quietHoursEnabled").value(false));

        verify(preferenceService, times(1)).getPreferences(TEST_USER_ID);
    }

    @Test
    void updatePreferences_Success() throws Exception {
        // Arrange
        NotificationPreferenceEntity updatedPreference = NotificationPreferenceEntity.builder().id("pref-123")
                .userId(TEST_USER_ID).emailEnabled(false).websocketEnabled(false).dueSoonNotifications(false)
                .overdueNotifications(true).taskAssignmentNotifications(true).boardSharingNotifications(false)
                .quietHoursEnabled(true).quietHoursStart(23).quietHoursEnd(8).build();

        when(preferenceService.updatePreferences(any(NotificationPreferenceEntity.class)))
                .thenReturn(updatedPreference);

        // Act & Assert
        mockMvc.perform(put("/preferences").principal(authentication).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPreference))).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pref-123")).andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.websocketEnabled").value(false))
                .andExpect(jsonPath("$.quietHoursEnabled").value(true))
                .andExpect(jsonPath("$.quietHoursStart").value(23)).andExpect(jsonPath("$.quietHoursEnd").value(8));

        verify(preferenceService, times(1)).updatePreferences(any(NotificationPreferenceEntity.class));
    }

    @Test
    void toggleEmailNotifications_Success() throws Exception {
        // Arrange
        NotificationPreferenceEntity updatedPreference = NotificationPreferenceEntity.builder().id("pref-123")
                .userId(TEST_USER_ID).emailEnabled(false) // Email now disabled
                .websocketEnabled(true).dueSoonNotifications(true).overdueNotifications(true)
                .taskAssignmentNotifications(true).boardSharingNotifications(true).quietHoursEnabled(false).build();

        when(preferenceService.setEmailEnabled(eq(TEST_USER_ID), eq(false))).thenReturn(updatedPreference);

        // Act & Assert
        mockMvc.perform(patch("/preferences/email/{enabled}", false).principal(authentication)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("pref-123"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID)).andExpect(jsonPath("$.emailEnabled").value(false)); 
                                                                                                                    
        verify(preferenceService, times(1)).setEmailEnabled(TEST_USER_ID, false);
    }

    @Test
    void toggleWebsocketNotifications_Success() throws Exception {
        // Arrange
        NotificationPreferenceEntity updatedPreference = NotificationPreferenceEntity.builder().id("pref-123")
                .userId(TEST_USER_ID).emailEnabled(true).websocketEnabled(false) // Websocket now disabled
                .dueSoonNotifications(true).overdueNotifications(true).taskAssignmentNotifications(true)
                .boardSharingNotifications(true).quietHoursEnabled(false).build();

        when(preferenceService.setWebsocketEnabled(eq(TEST_USER_ID), eq(false))).thenReturn(updatedPreference);

        // Act & Assert
        mockMvc.perform(patch("/preferences/websocket/{enabled}", false).principal(authentication)).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("pref-123"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.websocketEnabled").value(false)); // Verify websocket is now disabled

        verify(preferenceService, times(1)).setWebsocketEnabled(TEST_USER_ID, false);
    }

    @Test
    void configureQuietHours_Success() throws Exception {
        // Arrange
        NotificationPreferenceEntity updatedPreference = NotificationPreferenceEntity.builder().id("pref-123")
                .userId(TEST_USER_ID).emailEnabled(true).websocketEnabled(true).dueSoonNotifications(true)
                .overdueNotifications(true).taskAssignmentNotifications(true).boardSharingNotifications(true)
                .quietHoursEnabled(true) // Quiet hours now enabled
                .quietHoursStart(21).quietHoursEnd(8).build();

        when(preferenceService.configureQuietHours(eq(TEST_USER_ID), eq(true), eq(21), eq(8)))
                .thenReturn(updatedPreference);

        // Act & Assert
        mockMvc.perform(patch("/preferences/quiet-hours").principal(authentication).param("enabled", "true")
                .param("start", "21").param("end", "8")).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pref-123")).andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.quietHoursEnabled").value(true))
                .andExpect(jsonPath("$.quietHoursStart").value(21)).andExpect(jsonPath("$.quietHoursEnd").value(8));

        verify(preferenceService, times(1)).configureQuietHours(TEST_USER_ID, true, 21, 8);
    }

    @Test
    void configureQuietHours_InvalidHours() throws Exception {
        // Act & Assert - Invalid start hour (24 is out of range 0-23)
        mockMvc.perform(patch("/preferences/quiet-hours").principal(authentication).param("enabled", "true")
                .param("start", "24") // Invalid hour
                .param("end", "8")).andDo(print()).andExpect(status().isBadRequest());

        // Act & Assert - Invalid end hour (-1 is out of range 0-23)
        mockMvc.perform(patch("/preferences/quiet-hours").principal(authentication).param("enabled", "true")
                .param("start", "21").param("end", "-1")) // Invalid hour
                .andDo(print()).andExpect(status().isBadRequest());

        // No service method should be called with invalid hours
        verify(preferenceService, times(0)).configureQuietHours(anyString(), anyBoolean(), anyInt(), anyInt());
    }

    @Test
    void toggleNotificationType_Success() throws Exception {
        // Arrange
        NotificationPreferenceEntity updatedPreference = NotificationPreferenceEntity.builder().id("pref-123")
                .userId(TEST_USER_ID).emailEnabled(true).websocketEnabled(true).dueSoonNotifications(false)
                .overdueNotifications(true).taskAssignmentNotifications(true).boardSharingNotifications(true)
                .quietHoursEnabled(false).build();

        when(preferenceService.setNotificationTypeEnabled(eq(TEST_USER_ID), eq("DUE_SOON"), eq(false)))
                .thenReturn(updatedPreference);

        // Act & Assert
        mockMvc.perform(patch("/preferences/type/{type}/{enabled}", "DUE_SOON", false).principal(authentication))
                .andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("pref-123"))
                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.dueSoonNotifications").value(false)); // Verify DUE_SOON is now disabled

        verify(preferenceService, times(1)).setNotificationTypeEnabled(TEST_USER_ID, "DUE_SOON", false);
    }

    @Test
    void toggleNotificationType_InvalidType() throws Exception {
        // Arrange
        doThrow(new IllegalArgumentException("Unknown notification type: INVALID_TYPE")).when(preferenceService)
                .setNotificationTypeEnabled(eq(TEST_USER_ID), eq("INVALID_TYPE"), anyBoolean());

        // Act & Assert
        mockMvc.perform(patch("/preferences/type/{type}/{enabled}", "INVALID_TYPE", false).principal(authentication))
                .andDo(print()).andExpect(status().isBadRequest());

        verify(preferenceService, times(1)).setNotificationTypeEnabled(TEST_USER_ID, "INVALID_TYPE", false);
    }

    @Test
    void resetToDefaults_Success() throws Exception {
        // Arrange
        NotificationPreferenceEntity defaultPreference = NotificationPreferenceEntity.builder().id("pref-123")
                .userId(TEST_USER_ID).emailEnabled(true).websocketEnabled(true).dueSoonNotifications(true)
                .overdueNotifications(true).taskAssignmentNotifications(true).boardSharingNotifications(true)
                .quietHoursEnabled(false).build();

        when(preferenceService.resetToDefaults(TEST_USER_ID)).thenReturn(defaultPreference);

        // Act & Assert
        mockMvc.perform(post("/preferences/reset").principal(authentication)).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pref-123")).andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                .andExpect(jsonPath("$.emailEnabled").value(true)).andExpect(jsonPath("$.websocketEnabled").value(true))
                .andExpect(jsonPath("$.dueSoonNotifications").value(true))
                .andExpect(jsonPath("$.overdueNotifications").value(true))
                .andExpect(jsonPath("$.taskAssignmentNotifications").value(true))
                .andExpect(jsonPath("$.boardSharingNotifications").value(true))
                .andExpect(jsonPath("$.quietHoursEnabled").value(false));

        verify(preferenceService, times(1)).resetToDefaults(TEST_USER_ID);
    }
}