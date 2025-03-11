package com.phuonghieuto.backend.task_service.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Getter
public class FileKeyProvider {
    
    @Value("${auth.keys.public-key-path:${AUTH_PUBLIC_KEY_PATH:keys/public.pem}}")
    private String publicKeyPath;
    
    private String publicKey;
    
    @PostConstruct
    public void init() {
        try {
            publicKey = readFileContent(publicKeyPath);
            log.info("Successfully loaded public key from file");
        } catch (Exception e) {
            log.error("Failed to load public key from file", e);
            throw new RuntimeException("Failed to load public key", e);
        }
    }
    
    private String readFileContent(String path) throws Exception {
        Path filePath = Paths.get(path);
        return Files.readString(filePath);
    }
}