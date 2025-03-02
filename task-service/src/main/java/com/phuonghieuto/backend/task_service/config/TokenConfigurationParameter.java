package com.phuonghieuto.backend.task_service.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import com.phuonghieuto.backend.task_service.service.impl.FileKeyProvider;
import com.phuonghieuto.backend.task_service.util.KeyConverter;

import java.security.PublicKey;

/**
 * Configuration class named {@link TokenConfigurationParameter} for token
 * parameters. Provides access to token expiration settings and cryptographic
 * keys.
 */
@Getter
@Configuration
public class TokenConfigurationParameter {
        private final FileKeyProvider fileKeyProvider;
        private final PublicKey publicKey;

        public TokenConfigurationParameter(FileKeyProvider fileKeyProvider) {
                this.fileKeyProvider = fileKeyProvider;
                this.publicKey = KeyConverter.convertPublicKey(fileKeyProvider.getPublicKey());
        }

}
