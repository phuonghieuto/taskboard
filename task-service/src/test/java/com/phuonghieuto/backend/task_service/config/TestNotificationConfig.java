package com.phuonghieuto.backend.task_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.phuonghieuto.backend.task_service.messaging.producer.NotificationProducer;
import com.phuonghieuto.backend.task_service.model.collaboration.entity.BoardInvitationEntity;
import com.phuonghieuto.backend.task_service.model.task.entity.TaskEntity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@TestConfiguration
@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "false", matchIfMissing = false)
public class TestNotificationConfig {
    
    @Bean
    @Primary
    public NotificationProducer notificationProducer() {
        return new TestNotificationProducer();
    }
    
    // Test implementation that does nothing
    public static class TestNotificationProducer extends NotificationProducer {
        
        public TestNotificationProducer() {
            // Call parent constructor with null to avoid Spring trying to autowire
            super(null);
        }
        
        @Override
        public void sendTaskDueSoonNotification(TaskEntity task) {
            // Do nothing
        }

        @Override
        public void sendTaskOverdueNotification(TaskEntity task) {

        }

        @Override
        public void sendBoardInvitationNotification(BoardInvitationEntity invitation, String inviterName) {
            
        }
    }
}