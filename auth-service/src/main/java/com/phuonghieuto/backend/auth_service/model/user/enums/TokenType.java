package com.phuonghieuto.backend.auth_service.model.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum TokenType {

    BEARER("Bearer");

    private final String value;

}
