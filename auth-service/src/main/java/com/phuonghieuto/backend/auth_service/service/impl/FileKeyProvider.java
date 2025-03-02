package com.phuonghieuto.backend.auth_service.service.impl;

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
    @Value("${auth.keys.private-key-path:${AUTH_PRIVATE_KEY_PATH:keys/private.pem}}")
    private String privateKeyPath;
    
    private String publicKey;
    private String privateKey;
    
    @PostConstruct
    public void init() {
        try {
            publicKey = readFileContent(publicKeyPath);
            privateKey = readFileContent(privateKeyPath);
            log.info("Successfully loaded keys from files");
        } catch (Exception e) {
            log.error("Failed to load keys from files", e);
            throw new RuntimeException("Failed to load keys", e);
        }
    }
    
    private String readFileContent(String path) throws Exception {
        Path filePath = Paths.get(path);
        return Files.readString(filePath);
    }
}