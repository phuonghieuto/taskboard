package com.phuonghieuto.backend.auth_service.model.user.oauth2;

import com.phuonghieuto.backend.auth_service.exception.OAuth2AuthenticationProcessingException;
import com.phuonghieuto.backend.auth_service.model.user.enums.AuthProvider;
import com.phuonghieuto.backend.auth_service.model.user.oauth2.provider.GithubOAuth2UserInfo;
import com.phuonghieuto.backend.auth_service.model.user.oauth2.provider.GoogleOAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if(registrationId.equalsIgnoreCase(AuthProvider.GOOGLE.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.GITHUB.toString())) {
            return new GithubOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationProcessingException("Sorry! Login with " + registrationId + " is not supported yet.");
        }
    }
}