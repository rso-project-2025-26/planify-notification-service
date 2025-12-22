package com.planify.notification.dto;

import com.planify.notification.model.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {
    private String templateKey;
    private NotificationType type;
    private String subject;
    private String bodyTemplate;
    private String smsTemplate;
    private Boolean isActive;
    private String language;
}
