package com.phuonghieuto.backend.api_gateway.model;

import org.springframework.util.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Token {

    private String accessToken;
    private Long accessTokenExpiresAt;
    private String refreshToken;

    private static final String TOKEN_PREFIX = "Bearer ";

    public static boolean isBearerToken(final String authorizationHeader) {
        return StringUtils.hasText(authorizationHeader) &&
                authorizationHeader.startsWith(TOKEN_PREFIX);
    }

    public static String getJwt(final String authorizationHeader) {
        return authorizationHeader.replace(TOKEN_PREFIX, "");
    }

}
