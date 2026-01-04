package com.planify.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationDto {
    private UUID id;
    private UUID userId;
    private String title;
    private String message;
    private String notificationType;
    private UUID referenceId;
    private String referenceType;
    private String actionUrl;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
