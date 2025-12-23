package com.planify.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "notification.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SmsService {

    private final String accountSid;
    private final String authToken;
    private final String fromPhoneNumber;

    public SmsService(
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.phone-number}") String fromPhoneNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromPhoneNumber = fromPhoneNumber;
    }

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio SMS service initialized");
    }

    public String sendSms(String toPhoneNumber, String messageBody) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody).create();

            log.info("SMS sent successfully to {} with SID: {}", toPhoneNumber, message.getSid());
            return message.getSid();
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
