package com.phuonghieuto.backend.auth_service.config;

import com.phuonghieuto.backend.auth_service.messaging.producer.NotificationProducer;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
        public void sendEmailConfirmationMessage(UserEntity user) {
            // Do nothing in tests
            System.out.println("Test mode: Email confirmation skipped for " + user.getEmail());
        }
    }
}