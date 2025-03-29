package com.phuonghieuto.backend.auth_service.config;

import com.phuonghieuto.backend.auth_service.messaging.producer.NotificationProducer;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.Mockito;

@TestConfiguration
@Import({ TestRabbitMQConfig.class, TestNotificationConfig.class, TestRedisConfig.class })
public class TestConfig {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    @Primary
    public NotificationProducer notificationProducer() {
        NotificationProducer producer = Mockito.mock(NotificationProducer.class);
        // Set up the mock to do nothing when sendEmailConfirmationMessage is called
        Mockito.doNothing().when(producer).sendEmailConfirmationMessage(Mockito.any(UserEntity.class));
        return producer;
    }
}