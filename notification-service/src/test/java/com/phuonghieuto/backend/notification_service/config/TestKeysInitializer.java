package com.phuonghieuto.backend.notification_service.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public class TestKeysInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Set system properties for test
        System.setProperty("auth.keys.public-key-path", "classpath:keys/public.pem");
        System.setProperty("auth.keys.private-key-path", "classpath:keys/private.pem");
        System.setProperty("RABBITMQ_HOST", "localhost");
        System.setProperty("RABBITMQ_PORT", "5672");
        System.setProperty("RABBITMQ_USERNAME", "guest");
        System.setProperty("RABBITMQ_PASSWORD", "guest");
        
        System.out.println("Test using keys from classpath:keys/");
    }
}