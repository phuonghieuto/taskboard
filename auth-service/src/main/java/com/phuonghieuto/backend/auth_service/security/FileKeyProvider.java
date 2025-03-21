package com.phuonghieuto.backend.auth_service.security;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Getter
@RequiredArgsConstructor
public class FileKeyProvider {

    private final ResourceLoader resourceLoader;

    @Value("${auth.keys.public-key-path:/app/keys/public.pem}")
    private String publicKeyPath;

    @Value("${auth.keys.private-key-path:/app/keys/private.pem}")
    private String privateKeyPath;

    private String publicKey;
    private String privateKey;

    @PostConstruct
    public void init() {
        try {
            log.info("Loading keys from paths: public={}, private={}", publicKeyPath, privateKeyPath);
            publicKey = readFileContent(publicKeyPath);
            privateKey = readFileContent(privateKeyPath);
            log.info("Successfully loaded keys from files");
        } catch (Exception e) {
            log.error("Failed to load keys from files", e);
            throw new RuntimeException("Failed to load keys", e);
        }
    }

    private String readFileContent(String path) throws Exception {
        if (path.startsWith("classpath:")) {
            // Handle classpath resources
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Resource not found: " + path);
            }
            
            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } else {
            // Handle regular file paths
            Path filePath = Paths.get(path);
            log.debug("Attempting to read file from: {}, exists: {}", filePath.toAbsolutePath(), Files.exists(filePath));
            return Files.readString(filePath);
        }
    }
}