package com.planify.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private String recipientEmail;
    private String recipientPhone;
    private String templateKey;
    private Map<String, Object> variables;
}
