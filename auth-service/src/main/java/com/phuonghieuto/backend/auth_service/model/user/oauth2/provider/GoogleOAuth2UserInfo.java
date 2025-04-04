package com.phuonghieuto.backend.auth_service.model.user.oauth2.provider;

import java.util.Map;

import com.phuonghieuto.backend.auth_service.model.user.oauth2.OAuth2UserInfo;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getFirstName() {
        return (String) attributes.get("given_name");
    }
    
    @Override
    public String getLastName() {
        return (String) attributes.get("family_name");
    }
}