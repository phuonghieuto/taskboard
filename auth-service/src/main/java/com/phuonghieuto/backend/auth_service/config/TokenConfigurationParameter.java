package com.phuonghieuto.backend.auth_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.phuonghieuto.backend.auth_service.model.user.enums.ConfigurationParameter;
import com.phuonghieuto.backend.auth_service.service.impl.FileKeyProvider;
import com.phuonghieuto.backend.auth_service.util.KeyConverter;

import java.security.PrivateKey;
import java.security.PublicKey;

@Getter
@Configuration
public class TokenConfigurationParameter {

    private final FileKeyProvider fileKeyProvider;
    
    private final int accessTokenExpireMinute;
    private final int refreshTokenExpireDay;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    @Autowired
    public TokenConfigurationParameter(FileKeyProvider fileKeyProvider) {
        this.fileKeyProvider = fileKeyProvider;
        
        this.accessTokenExpireMinute = Integer.parseInt(
                ConfigurationParameter.AUTH_ACCESS_TOKEN_EXPIRE_MINUTE.getDefaultValue()
        );

        this.refreshTokenExpireDay = Integer.parseInt(
                ConfigurationParameter.AUTH_REFRESH_TOKEN_EXPIRE_DAY.getDefaultValue()
        );

        // Use keys from files instead of hardcoded values
        this.publicKey = KeyConverter.convertPublicKey(fileKeyProvider.getPublicKey());
        this.privateKey = KeyConverter.convertPrivateKey(fileKeyProvider.getPrivateKey());
    }
}