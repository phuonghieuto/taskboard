package com.phuonghieuto.backend.auth_service.model.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ConfigurationParameter {

    AUTH_ACCESS_TOKEN_EXPIRE_MINUTE("720"),
    AUTH_REFRESH_TOKEN_EXPIRE_DAY("7"),
    AUTH_PUBLIC_KEY("public_key"),  // Just a placeholder
    AUTH_PRIVATE_KEY("private_key"); // Just a placeholder

    private final String defaultValue;
}