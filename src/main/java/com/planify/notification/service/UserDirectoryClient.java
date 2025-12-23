package com.planify.notification.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserDirectoryClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${user-service.base-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${user-service.user-endpoint:/api/users/{id}}")
    private String userEndpoint;

    public String getUserPhone(UUID userId) {
        String url = baseUrl + userEndpoint.replace("{id}", userId.toString());
        try {
            ResponseEntity<UserResponse> response = restTemplate.getForEntity(url, UserResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (response.getBody().getSmsConsent()) {
                    return response.getBody().getPhoneNumber();
                } else {
                    return null;
                }
            }
            log.warn("User service returned non-OK for {}: {}", userId, response.getStatusCode());
        } catch (RestClientException ex) {
            log.error("Failed to fetch user {} from user-service", userId, ex);
        }
        return null;
    }

    @Data
    public static class UserResponse {
        private String email;
        private Boolean emailConsent;
        private String phoneNumber;
        private Boolean smsConsent;
        private String firstName;
        private String lastName;
    }
}
