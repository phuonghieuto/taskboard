package com.phuonghieuto.backend.auth_service.model.user.oauth2.provider;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.phuonghieuto.backend.auth_service.model.user.oauth2.OAuth2UserInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GithubOAuth2UserInfo extends OAuth2UserInfo {

    public GithubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return ((Integer) attributes.get("id")).toString();
    }

        @Override
    public String getEmail() {
        // Try to get direct email first
        String email = (String) attributes.get("email");
        if (email != null && !email.isEmpty()) {
            log.debug("Found email directly in attributes: {}", email);
            return email;
        }
    
        log.debug("Email not found in primary attributes, will attempt to fetch from emails endpoint");
        log.debug("Available attribute keys: {}", attributes.keySet());
    
        // If email is private, we need to make an additional API call
        try {
            String accessToken = (String) attributes.get("access_token");
            if (accessToken == null) {
                log.warn("No access token found in attributes, unable to fetch GitHub emails");
                return null;
            }
            
            log.debug("Using access token to fetch GitHub emails: {}", accessToken.substring(0, 5) + "...");
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/vnd.github.v3+json");
            
            // Add proper error handling with response entity
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "https://api.github.com/user/emails", 
                HttpMethod.GET, 
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> emails = response.getBody();
                log.debug("GitHub emails API response: {}", emails);
                
                if (emails != null && !emails.isEmpty()) {
                    // Processing logic remains the same...
                    // Return primary verified email, then any verified, then first email
                    return findBestEmail(emails);
                }
            } else {
                log.error("GitHub API returned error status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error retrieving GitHub user emails", e);
        }
        
        log.warn("No email found in GitHub user attributes or via email API");
        return null;
    }
    
    private String findBestEmail(List<Map<String, Object>> emails) {
        // First look for primary email
        for (Map<String, Object> emailObj : emails) {
            Boolean isPrimary = (Boolean) emailObj.get("primary");
            Boolean isVerified = (Boolean) emailObj.get("verified");
            
            if (Boolean.TRUE.equals(isPrimary) && Boolean.TRUE.equals(isVerified)) {
                String email = (String) emailObj.get("email");
                log.debug("Found primary verified email: {}", email);
                return email;
            }
        }
        
        // If no primary email found, take first verified email
        for (Map<String, Object> emailObj : emails) {
            Boolean isVerified = (Boolean) emailObj.get("verified");
            if (Boolean.TRUE.equals(isVerified)) {
                String email = (String) emailObj.get("email");
                log.debug("Found verified email: {}", email);
                return email;
            }
        }
        
        // If no verified email found, take the first one
        if (!emails.isEmpty() && emails.get(0).get("email") != null) {
            String email = (String) emails.get(0).get("email");
            log.debug("Using first available email: {}", email);
            return email;
        }
        
        return null;
    }

    @Override
    public String getFirstName() {
        String name = (String) attributes.get("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }
        return name;
    }

    @Override
    public String getLastName() {
        String name = (String) attributes.get("name");
        if (name != null && name.contains(" ")) {
            return name.split(" ")[1];
        }
        return "";
    }
}