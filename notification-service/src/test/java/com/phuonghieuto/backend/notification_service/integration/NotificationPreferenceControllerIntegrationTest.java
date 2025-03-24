package com.phuonghieuto.backend.notification_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phuonghieuto.backend.notification_service.config.TestTokenConfigurationParameter;
import com.phuonghieuto.backend.notification_service.model.auth.enums.TokenClaims;
import com.phuonghieuto.backend.notification_service.model.preference.entity.NotificationPreferenceEntity;
import com.phuonghieuto.backend.notification_service.repository.NotificationPreferenceRepository;
import io.jsonwebtoken.Jwts;
@AutoConfigureMockMvc
public class NotificationPreferenceControllerIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private NotificationPreferenceRepository preferenceRepository;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TestTokenConfigurationParameter tokenConfigurationParameter;

        private static final String TEST_USER_ID = "test-user-id";
        private static final String TEST_USER_EMAIL = "test@example.com";

        private String accessToken;

        @BeforeEach
        void setUp() {
                // Clean up the database before each test
                preferenceRepository.deleteAll();

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

        private NotificationPreferenceEntity createPreference(String userId) {
                NotificationPreferenceEntity preference = NotificationPreferenceEntity.builder()
                                .userId(userId)
                                .emailEnabled(true)
                                .websocketEnabled(true)
                                .dueSoonNotifications(true)
                                .overdueNotifications(true)
                                .taskAssignmentNotifications(true)
                                .boardSharingNotifications(true)
                                .quietHoursEnabled(false)
                                .quietHoursStart(22)
                                .quietHoursEnd(7)
                                .build();
                return preferenceRepository.save(preference);
        }

        @Test
        void getUserPreferences_ExistingUser_Success() throws Exception {
                // Create preference for the test user
                NotificationPreferenceEntity savedPreference = createPreference(TEST_USER_ID);

                // Get preferences
                MvcResult result = mockMvc.perform(get("/preferences")
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.emailEnabled").value(true))
                                .andExpect(jsonPath("$.websocketEnabled").value(true))
                                .andExpect(jsonPath("$.dueSoonNotifications").value(true))
                                .andExpect(jsonPath("$.overdueNotifications").value(true))
                                .andExpect(jsonPath("$.taskAssignmentNotifications").value(true))
                                .andExpect(jsonPath("$.boardSharingNotifications").value(true))
                                .andExpect(jsonPath("$.quietHoursEnabled").value(false))
                                .andExpect(jsonPath("$.quietHoursStart").value(22))
                                .andExpect(jsonPath("$.quietHoursEnd").value(7))
                                .andReturn();

                // Verify response
                String responseJson = result.getResponse().getContentAsString();
                NotificationPreferenceEntity preference = objectMapper.readValue(responseJson, NotificationPreferenceEntity.class);
                
                assertEquals(savedPreference.getId(), preference.getId());
                assertEquals(TEST_USER_ID, preference.getUserId());
                assertTrue(preference.isEmailEnabled());
                assertTrue(preference.isWebsocketEnabled());
                assertFalse(preference.isQuietHoursEnabled());
        }

        @Test
        void getUserPreferences_NewUser_CreatesDefaultPreferences() throws Exception {
                // Get preferences for a user that doesn't exist yet
                MvcResult result = mockMvc.perform(get("/preferences")
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.emailEnabled").value(true))
                                .andExpect(jsonPath("$.websocketEnabled").value(true))
                                .andExpect(jsonPath("$.dueSoonNotifications").value(true))
                                .andExpect(jsonPath("$.overdueNotifications").value(true))
                                .andExpect(jsonPath("$.taskAssignmentNotifications").value(true))
                                .andExpect(jsonPath("$.boardSharingNotifications").value(true))
                                .andExpect(jsonPath("$.quietHoursEnabled").value(false))
                                .andReturn();

                // Verify response and database
                String responseJson = result.getResponse().getContentAsString();
                NotificationPreferenceEntity preference = objectMapper.readValue(responseJson, NotificationPreferenceEntity.class);
                
                assertNotNull(preference.getId());
                assertEquals(TEST_USER_ID, preference.getUserId());
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertEquals(preference.getId(), savedPreference.get().getId());
        }

        @Test
        void updatePreferences_Success() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Create updated preference
                NotificationPreferenceEntity updateRequest = NotificationPreferenceEntity.builder()
                                .userId(TEST_USER_ID) // This should be overwritten by the controller for security
                                .emailEnabled(false)
                                .websocketEnabled(false)
                                .dueSoonNotifications(false)
                                .overdueNotifications(true)
                                .taskAssignmentNotifications(false)
                                .boardSharingNotifications(false)
                                .quietHoursEnabled(true)
                                .quietHoursStart(21)
                                .quietHoursEnd(8)
                                .build();

                // Update preferences
                MvcResult result = mockMvc.perform(put("/preferences")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.emailEnabled").value(false))
                                .andExpect(jsonPath("$.websocketEnabled").value(false))
                                .andExpect(jsonPath("$.dueSoonNotifications").value(false))
                                .andExpect(jsonPath("$.overdueNotifications").value(true))
                                .andExpect(jsonPath("$.taskAssignmentNotifications").value(false))
                                .andExpect(jsonPath("$.boardSharingNotifications").value(false))
                                .andExpect(jsonPath("$.quietHoursEnabled").value(true))
                                .andExpect(jsonPath("$.quietHoursStart").value(21))
                                .andExpect(jsonPath("$.quietHoursEnd").value(8))
                                .andReturn();

                // Verify database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertEquals(TEST_USER_ID, savedPreference.get().getUserId());
                assertFalse(savedPreference.get().isEmailEnabled());
                assertFalse(savedPreference.get().isWebsocketEnabled());
                assertTrue(savedPreference.get().isQuietHoursEnabled());
                assertEquals(21, savedPreference.get().getQuietHoursStart());
                assertEquals(8, savedPreference.get().getQuietHoursEnd());
        }

        @Test
        void updatePreferences_AttemptingToChangeUserIdIgnored() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Create update with wrong userId
                NotificationPreferenceEntity updateRequest = NotificationPreferenceEntity.builder()
                                .userId("malicious-attempt")
                                .emailEnabled(false)
                                .websocketEnabled(false)
                                .build();

                // Update preferences
                mockMvc.perform(put("/preferences")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID)) // userId should be from JWT, not request
                                .andExpect(jsonPath("$.emailEnabled").value(false))
                                .andExpect(jsonPath("$.websocketEnabled").value(false));

                // Verify database has correct userId
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertEquals(TEST_USER_ID, savedPreference.get().getUserId());
                
                // Verify no record with malicious userId
                Optional<NotificationPreferenceEntity> maliciousPreference = preferenceRepository.findByUserId("malicious-attempt");
                assertFalse(maliciousPreference.isPresent());
        }

        @Test
        void toggleEmailNotifications_Success() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Toggle email notifications off
                mockMvc.perform(patch("/preferences/email/{enabled}", false)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.emailEnabled").value(false));
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertFalse(savedPreference.get().isEmailEnabled());
                
                // Toggle email notifications back on
                mockMvc.perform(patch("/preferences/email/{enabled}", true)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.emailEnabled").value(true));
                
                // Verify in database
                savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertTrue(savedPreference.get().isEmailEnabled());
        }

        @Test
        void toggleWebsocketNotifications_Success() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Toggle websocket notifications off
                mockMvc.perform(patch("/preferences/websocket/{enabled}", false)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.websocketEnabled").value(false));
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertFalse(savedPreference.get().isWebsocketEnabled());
        }

        @Test
        void configureQuietHours_Success() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Configure quiet hours
                mockMvc.perform(patch("/preferences/quiet-hours")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("enabled", "true")
                                .param("start", "23")
                                .param("end", "6"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.quietHoursEnabled").value(true))
                                .andExpect(jsonPath("$.quietHoursStart").value(23))
                                .andExpect(jsonPath("$.quietHoursEnd").value(6));
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertTrue(savedPreference.get().isQuietHoursEnabled());
                assertEquals(23, savedPreference.get().getQuietHoursStart());
                assertEquals(6, savedPreference.get().getQuietHoursEnd());
        }

        @Test
        void configureQuietHours_InvalidHours() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Configure with invalid start hour (24)
                mockMvc.perform(patch("/preferences/quiet-hours")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("enabled", "true")
                                .param("start", "24") // Invalid hour
                                .param("end", "6"))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
                
                // Configure with invalid end hour (-1)
                mockMvc.perform(patch("/preferences/quiet-hours")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("enabled", "true")
                                .param("start", "22")
                                .param("end", "-1")) // Invalid hour
                                .andDo(print())
                                .andExpect(status().isBadRequest());
                
                // Verify preferences were not changed
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertFalse(savedPreference.get().isQuietHoursEnabled());
                assertEquals(22, savedPreference.get().getQuietHoursStart());
                assertEquals(7, savedPreference.get().getQuietHoursEnd());
        }

        @Test
        void configureQuietHours_DisablingOnly() throws Exception {
                // Create preference with quiet hours enabled
                NotificationPreferenceEntity preference = NotificationPreferenceEntity.builder()
                                .userId(TEST_USER_ID)
                                .emailEnabled(true)
                                .websocketEnabled(true)
                                .quietHoursEnabled(true)
                                .quietHoursStart(22)
                                .quietHoursEnd(7)
                                .build();
                preferenceRepository.save(preference);
                
                // Disable quiet hours without changing start/end
                mockMvc.perform(patch("/preferences/quiet-hours")
                                .header("Authorization", "Bearer " + accessToken)
                                .param("enabled", "false"))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.quietHoursEnabled").value(false))
                                .andExpect(jsonPath("$.quietHoursStart").value(22))
                                .andExpect(jsonPath("$.quietHoursEnd").value(7));
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertFalse(savedPreference.get().isQuietHoursEnabled());
                assertEquals(22, savedPreference.get().getQuietHoursStart());
                assertEquals(7, savedPreference.get().getQuietHoursEnd());
        }

        @Test
        void toggleNotificationType_Success() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Toggle due soon notifications off
                mockMvc.perform(patch("/preferences/type/{type}/{enabled}", "DUE_SOON", false)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.dueSoonNotifications").value(false));
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertFalse(savedPreference.get().isDueSoonNotifications());
                
                // Toggle overdue notifications off
                mockMvc.perform(patch("/preferences/type/{type}/{enabled}", "OVERDUE", false)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.overdueNotifications").value(false));
                
                savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertFalse(savedPreference.get().isOverdueNotifications());
        }

        @Test
        void toggleNotificationType_InvalidType() throws Exception {
                // Create preference for the test user
                createPreference(TEST_USER_ID);
                
                // Toggle invalid notification type
                mockMvc.perform(patch("/preferences/type/{type}/{enabled}", "INVALID_TYPE", false)
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isBadRequest());
        }

        @Test
        void resetToDefaults_Success() throws Exception {
                // Create preference with non-default values
                NotificationPreferenceEntity preference = NotificationPreferenceEntity.builder()
                                .userId(TEST_USER_ID)
                                .emailEnabled(false)
                                .websocketEnabled(false)
                                .dueSoonNotifications(false)
                                .overdueNotifications(false)
                                .taskAssignmentNotifications(false)
                                .boardSharingNotifications(false)
                                .quietHoursEnabled(true)
                                .quietHoursStart(20)
                                .quietHoursEnd(10)
                                .build();
                preference = preferenceRepository.save(preference);
                
                // Reset to defaults
                mockMvc.perform(post("/preferences/reset")
                                .header("Authorization", "Bearer " + accessToken))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.userId").value(TEST_USER_ID))
                                .andExpect(jsonPath("$.emailEnabled").value(true))
                                .andExpect(jsonPath("$.websocketEnabled").value(true))
                                .andExpect(jsonPath("$.dueSoonNotifications").value(true))
                                .andExpect(jsonPath("$.overdueNotifications").value(true))
                                .andExpect(jsonPath("$.taskAssignmentNotifications").value(true))
                                .andExpect(jsonPath("$.boardSharingNotifications").value(true))
                                .andExpect(jsonPath("$.quietHoursEnabled").value(false));
                
                // Verify in database
                Optional<NotificationPreferenceEntity> savedPreference = preferenceRepository.findByUserId(TEST_USER_ID);
                assertTrue(savedPreference.isPresent());
                assertTrue(savedPreference.get().isEmailEnabled());
                assertTrue(savedPreference.get().isWebsocketEnabled());
                assertTrue(savedPreference.get().isDueSoonNotifications());
                assertTrue(savedPreference.get().isOverdueNotifications());
                assertTrue(savedPreference.get().isTaskAssignmentNotifications());
                assertTrue(savedPreference.get().isBoardSharingNotifications());
                assertFalse(savedPreference.get().isQuietHoursEnabled());
        }

        @Test
        void allEndpoints_Unauthorized() throws Exception {
                // Get preferences - unauthorized
                mockMvc.perform(get("/preferences"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Update preferences - unauthorized 
                mockMvc.perform(put("/preferences")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Toggle email - unauthorized
                mockMvc.perform(patch("/preferences/email/{enabled}", true))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Toggle websocket - unauthorized
                mockMvc.perform(patch("/preferences/websocket/{enabled}", true))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Configure quiet hours - unauthorized
                mockMvc.perform(patch("/preferences/quiet-hours")
                                .param("enabled", "true"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Toggle notification type - unauthorized
                mockMvc.perform(patch("/preferences/type/{type}/{enabled}", "DUE_SOON", true))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
                
                // Reset to defaults - unauthorized
                mockMvc.perform(post("/preferences/reset"))
                                .andDo(print())
                                .andExpect(status().isUnauthorized());
        }
}
