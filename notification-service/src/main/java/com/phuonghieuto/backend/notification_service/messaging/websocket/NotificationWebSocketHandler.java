package com.phuonghieuto.backend.notification_service.messaging.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    
    public NotificationWebSocketHandler() {
        // Initialize and configure ObjectMapper with JavaTimeModule
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            // Check if there's already a session for this user
            WebSocketSession existingSession = userSessions.get(userId);
            if (existingSession != null && existingSession.isOpen()) {
                // Close the old session
                try {
                    existingSession.close(CloseStatus.NORMAL.withReason("Session replaced by new connection"));
                    log.info("Closed previous session for user: {}", userId);
                } catch (IOException e) {
                    log.error("Error closing previous session: {}", e.getMessage());
                }
            }
            
            userSessions.put(userId, session);
            allSessions.put(session.getId(), session);
            log.info("WebSocket connection established: {} for user: {}", session.getId(), userId);
        } else {
            log.warn("No userId found in session attributes");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication failed"));
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
        }
        allSessions.remove(session.getId());
        log.info("WebSocket connection closed: {} for user: {} with status: {}", 
                session.getId(), userId, status);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Handle incoming messages if needed
        log.info("Received message: {}", message.getPayload());
        
        // Echo back the message for testing purposes
        session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("Transport error for session {}: {}", session.getId(), exception.getMessage(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("Transport error"));
        }
    }
    
    private String extractUserId(WebSocketSession session) {
        // Get userId from session attributes (set by the interceptor)
        return (String) session.getAttributes().get("userId");
    }
    
    // Methods to send notifications to specific users or broadcast
    public void sendToUser(String userId, Object notification) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("Sent message to user {}: {}", userId, jsonMessage);
            } catch (IOException e) {
                log.error("Failed to send message to user {}", userId, e);
            }
        } else {
            log.warn("User {} is not connected to WebSocket", userId);
        }
    }
    
    public void broadcast(Object notification) {
        String message;
        try {
            message = objectMapper.writeValueAsString(notification);
            log.info("Broadcasting message to {} sessions", allSessions.size());
            for (WebSocketSession session : allSessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        log.error("Failed to send broadcast message to session {}", session.getId(), e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to serialize broadcast message", e);
        }
    }

    // For debugging
    public Map<String, WebSocketSession> getActiveSessions() {
        return userSessions;
    }
}