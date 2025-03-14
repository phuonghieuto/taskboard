package com.phuonghieuto.backend.auth_service.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.phuonghieuto.backend.auth_service.util.CookieUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage(), exception);
        
        // Log request details that might help diagnose the issue
        log.debug("Request URI: {}", request.getRequestURI());
        log.debug("Request URL: {}", request.getRequestURL());
        log.debug("Request parameters: {}", request.getParameterMap());
        
        String targetUrl = CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(cookie -> {
                    log.debug("Found redirect_uri cookie: {}", cookie.getValue());
                    return cookie.getValue();
                })
                .orElse("/");
        
        log.debug("Target URL before adding error param: {}", targetUrl);

        targetUrl = UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("error", exception.getLocalizedMessage())
                .queryParam("error_description", exception.getMessage())
                .build().toUriString();
        
        log.debug("Final redirect URL with error: {}", targetUrl);

        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}