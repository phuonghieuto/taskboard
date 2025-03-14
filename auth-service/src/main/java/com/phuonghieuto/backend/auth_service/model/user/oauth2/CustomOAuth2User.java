package com.phuonghieuto.backend.auth_service.model.user.oauth2;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;

import lombok.Getter;

@Getter
public class CustomOAuth2User implements OAuth2User {
    private String id;
    private String email;
    private Collection<? extends GrantedAuthority> authorities;
    private Map<String, Object> attributes;

    private CustomOAuth2User(String id, String email, Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes) {
        this.id = id;
        this.email = email;
        this.authorities = authorities;
        this.attributes = attributes;
    }

    public static CustomOAuth2User create(UserEntity user, Map<String, Object> attributes) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
            new SimpleGrantedAuthority(user.getUserType().name())
        );
        
        return new CustomOAuth2User(
            user.getId(),
            user.getEmail(),
            authorities,
            attributes
        );
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return id;
    }
}