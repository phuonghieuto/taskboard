package com.phuonghieuto.backend.auth_service.security;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.phuonghieuto.backend.auth_service.exception.BadRequestException;
import com.phuonghieuto.backend.auth_service.model.Token;
import com.phuonghieuto.backend.auth_service.model.user.entity.UserEntity;
import com.phuonghieuto.backend.auth_service.model.user.oauth2.CustomOAuth2User;
import com.phuonghieuto.backend.auth_service.repository.UserRepository;
import com.phuonghieuto.backend.auth_service.service.TokenGenerationService;
import com.phuonghieuto.backend.auth_service.util.CookieUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Import statements remain the same

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenGenerationService tokenGenerationService;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${app.oauth2.authorizedRedirectUrisString}")
    private String authorizedRedirectUrisString;

    private List<String> getAuthorizedRedirectUris() {
        return Arrays.asList(authorizedRedirectUrisString.split(","));
    }

    

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 authentication success for provider: {}",
                request.getRequestURL().toString() + "?" + request.getQueryString());
        log.info("Authentication principal type: {}", authentication.getPrincipal().getClass().getName());

        try {
            String targetUrl = determineTargetUrl(request, response, authentication);
            log.info("Redirecting to target URL: {}", targetUrl);
            if (response.isCommitted()) {
                log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
                return;
            }

            clearAuthenticationAttributes(request);
            httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            log.error("Error in OAuth2 authentication success handler", e);
            getRedirectStrategy().sendRedirect(request, response, "/api/v1/oauth2/debug?error=" + e.getMessage());
        }
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        log.debug("Determining target URL for OAuth2 success");
        Optional<String> redirectUri = CookieUtils
                .getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(cookie -> {
                    log.debug("Found redirect cookie: {}", cookie.getValue());
                    return cookie.getValue();
                });

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            log.warn("Unauthorized redirect URI: {}", redirectUri.get());
            throw new BadRequestException(
                    "Sorry! We've got an Unauthorized Redirect URI and can't proceed with the authentication");
        }

        String targetUrl = redirectUri.orElse("http://localhost:8080/api/v1/oauth2/redirect");
        log.info("Determined target URL: {}", targetUrl);

        try {
            CustomOAuth2User userPrincipal = (CustomOAuth2User) authentication.getPrincipal();
            log.debug("OAuth2 user email: {}", userPrincipal.getEmail());

            if (userPrincipal.getEmail() == null || userPrincipal.getEmail().isEmpty()) {
                log.error("OAuth2 authentication failed: Email is missing from user principal");
                throw new RuntimeException(
                        "No email provided by OAuth provider. Please ensure your GitHub account has a valid email.");
            }

            // Find the user entity
            UserEntity user = userRepository.findByEmail(userPrincipal.getEmail()).orElseThrow(() -> {
                log.error("User not found with email: {}", userPrincipal.getEmail());
                return new RuntimeException("User not found with email: " + userPrincipal.getEmail());
            });

            // Generate JWT token
            Token token = tokenGenerationService.generateToken(user.getClaims());
            log.debug("Generated tokens for user: {}", user.getEmail());

            return UriComponentsBuilder.fromUriString(targetUrl).queryParam("token", token.getAccessToken())
                    .queryParam("refresh_token", token.getRefreshToken()).build().toUriString();
        } catch (Exception e) {
            log.error("Error while determining target URL", e);
            return UriComponentsBuilder.fromUriString("/api/v1/oauth2/debug").queryParam("error", e.getMessage())
                    .build().toUriString();
        }
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);
        return getAuthorizedRedirectUris().stream().anyMatch(authorizedRedirectUri -> {
            URI authorizedURI = URI.create(authorizedRedirectUri);
            return authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                    && authorizedURI.getPort() == clientRedirectUri.getPort();
        });
    }
}