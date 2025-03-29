package com.phuonghieuto.backend.task_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;

@Configuration
@Getter
public class RedisProperties {
    private int redisPort;
    private String redisHost;

    public RedisProperties(
      @Value("${spring.redis.port}") int redisPort, 
      @Value("${spring.redis.host}") String redisHost) {
        this.redisPort = redisPort;
        this.redisHost = redisHost;
    }
}