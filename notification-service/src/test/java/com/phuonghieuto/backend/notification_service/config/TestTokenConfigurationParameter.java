package com.phuonghieuto.backend.notification_service.config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.phuonghieuto.backend.notification_service.security.FileKeyProvider;
import com.phuonghieuto.backend.notification_service.util.KeyConverter;

@Configuration
public class TestTokenConfigurationParameter extends TokenConfigurationParameter {

    @Value("${auth.keys.private-key-path:/app/keys/private.pem}")
    private String privateKeyPath;
    
    @Autowired
    private ResourceLoader resourceLoader;

    
    @Autowired
    public TestTokenConfigurationParameter(FileKeyProvider fileKeyProvider) {
        super(fileKeyProvider);
    }

    @Override
    public PublicKey getPublicKey() {
        return super.getPublicKey();
    }

    public PrivateKey getPrivateKey() {
        try {
            String privateKey = readFileContent(privateKeyPath);
            return KeyConverter.convertPrivateKey(privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key", e);
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
            return Files.readString(filePath);
        }
    }
}
