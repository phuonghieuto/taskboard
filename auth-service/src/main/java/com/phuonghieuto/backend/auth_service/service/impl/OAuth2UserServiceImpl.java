package com.phuonghieuto.backend.auth_service.service.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.phuonghieuto.backend.auth_service.exception.OAuth2AuthenticationProcessingException;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.enums.AuthProvider;
import com.phuonghieuto.backend.auth_service.model.user.oauth2.CustomOAuth2User;
import com.phuonghieuto.backend.auth_service.model.user.oauth2.OAuth2UserInfo;
import com.phuonghieuto.backend.auth_service.model.user.oauth2.OAuth2UserInfoFactory;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        log.info("OAuth2 provider returned user: {}", oAuth2User);
        try {
            if (oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase().equals(AuthProvider.GITHUB.name())) {
            Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
            attributes.put("access_token", oAuth2UserRequest.getAccessToken().getTokenValue());
            oAuth2User = new DefaultOAuth2User(
                oAuth2User.getAuthorities(), 
                attributes, 
                oAuth2UserRequest.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName()
            );
            log.info("Added access token to GitHub OAuth2 user attributes");
        }
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error processing OAuth2 user", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        AuthProvider authProvider = AuthProvider
                .valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase());

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(authProvider.name(),
                oAuth2User.getAttributes());

        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        UserEntity user = userRepository.findByEmail(oAuth2UserInfo.getEmail()).orElse(null);
        log.info("User found by email: {}", user);
        if (user != null) {
            if (!user.getProvider().equals(authProvider)) {
                throw new OAuth2AuthenticationProcessingException("You're signed up with " + user.getProvider()
                        + ". Please use your " + user.getProvider() + " account to login");
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
            log.info("New user registered: {}", user.getEmail());
        }

        return CustomOAuth2User.create(user, oAuth2User.getAttributes());
    }

    private UserEntity registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        UserEntity user = new UserEntity();

        user.setProvider(
                AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setFirstName(oAuth2UserInfo.getFirstName());
        user.setLastName(oAuth2UserInfo.getLastName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setEmailConfirmed(true); // Email is verified by the provider

        log.info("Registering new OAuth2 user: {}", user.getEmail());
        return userRepository.save(user);
    }

    private UserEntity updateExistingUser(UserEntity user, OAuth2UserInfo oAuth2UserInfo) {
        try {
            user.setFirstName(oAuth2UserInfo.getFirstName());
            user.setLastName(oAuth2UserInfo.getLastName());
            log.info("Updating existing OAuth2 user: {}", user.getEmail());
            return userRepository.save(user);
        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Concurrent update detected for user: {}, retrying...", user.getEmail());
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }
}