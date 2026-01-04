package com.planify.notification.service;

import com.vonage.client.VonageClient;
import com.vonage.client.sms.messages.TextMessage;
import lombok.extern.slf4j.Slf4j;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
public class SmsService {

    @Value("${vonage.api-key}")
    private String apiKey;

    @Value("${vonage.api-secret}")
    private String apiSecret;

    @Value("${vonage.phone-number}")
    private String fromPhoneNumber;

    private VonageClient client;

    @PostConstruct
    public void init() {
        client = VonageClient.builder()
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .build();
        log.info("Vonage SMS service initialized");
    }

    @Retry(name = "smsService")
    public String sendSms(String toPhoneNumber, String messageBody) {
        try {
            TextMessage message = new TextMessage(fromPhoneNumber, toPhoneNumber, messageBody);
            client.getSmsClient().submitMessage(message);
            log.info("SMS sent successfully to {} with SID: {}", toPhoneNumber, message.getEntityId());
            return message.getEntityId();
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS", e);
        }
    }

    public String sendSms(String toPhoneNumber, String messageBody, int maxLength) {
        String truncatedMessage = messageBody.length() > maxLength
                ? messageBody.substring(0, maxLength - 3) + "..."
                : messageBody;
        return sendSms(toPhoneNumber, truncatedMessage);
    }
}
