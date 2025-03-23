package com.phuonghieuto.backend.task_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@Import({ TestRabbitMQConfig.class, TestNotificationConfig.class })
public class TestConfig {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // @Bean
    // @Primary
    // public NotificationProducer notificationProducer() {
    //     NotificationProducer producer = Mockito.mock(NotificationProducer.class);
    //     // Set up the mock to do nothing when sendEmailConfirmationMessage is called
    //     return producer;
    // }
}