package com.phuonghieuto.backend.user_service.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import com.phuonghieuto.backend.user_service.model.user.enums.ConfigurationParameter;
import com.phuonghieuto.backend.user_service.util.KeyConverter;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * Configuration class named {@link TokenConfigurationParameter} for token parameters.
 * Provides access to token expiration settings and cryptographic keys.
 */
@Getter
@Configuration
public class TokenConfigurationParameter {

    private final int accessTokenExpireDay;
    private final int refreshTokenExpireDay;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;

    public TokenConfigurationParameter() {

        this.accessTokenExpireDay = Integer.parseInt(
                ConfigurationParameter.AUTH_ACCESS_TOKEN_EXPIRE_DAY.getDefaultValue()
        );

        this.refreshTokenExpireDay = Integer.parseInt(
                ConfigurationParameter.AUTH_REFRESH_TOKEN_EXPIRE_DAY.getDefaultValue()
        );

        this.publicKey = KeyConverter.convertPublicKey(
                ConfigurationParameter.AUTH_PUBLIC_KEY.getDefaultValue()
        );

        this.privateKey = KeyConverter.convertPrivateKey(
                ConfigurationParameter.AUTH_PRIVATE_KEY.getDefaultValue()
        );

    }

}
