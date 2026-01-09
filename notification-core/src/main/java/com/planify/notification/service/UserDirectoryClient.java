package com.planify.notification.service;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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

    private final RestTemplate restTemplate;

    @Value("${user-service.base-url:http://localhost:8082}")
    private String baseUrl;

    @Value("${user-service.user-endpoint:/api/users/{id}}")
    private String userEndpoint;

    @Retry(name = "defaultRetry")
    @Bulkhead(name = "defaultBulkhead")
    @CircuitBreaker(name = "defaultCircuitBreaker", fallbackMethod = "getUserPhoneFallback")
    public UserResponse getUser(UUID userId) {
        log.info("Fetching user for userId: {}", userId);
        String url = baseUrl + userEndpoint.replace("{id}", userId.toString());
        try {
            ResponseEntity<UserResponse> response = restTemplate.getForEntity(url, UserResponse.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            log.warn("User service returned non-OK for {}: {}", userId, response.getStatusCode());
        } catch (RestClientException ex) {
            log.error("Failed to fetch user {} from user-service", userId, ex);
            throw ex; // Re-throw, da sprožimo circuit breake
        }
        return null;
    }

    private UserResponse getUserPhoneFallback(UUID userId, Exception ex) {
        log.error("User-service is unavailable. Cannot fetch phone for user {}. Error: {}",
                  userId, ex.getMessage());
        // Vračamo null, notifikacija ne bo poslana
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
