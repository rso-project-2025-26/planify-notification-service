package com.planify.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "in_app_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "notification_type", length = 50)
    private String notificationType; // invitation, join_request, event_update, etc.

    @Column(name = "reference_id")
    private UUID referenceId; // ID of the related entity (invitation, join request, etc.)

    @Column(name = "reference_type", length = 50)
    private String referenceType; // invitation, join_request, event, etc.

    @Column(name = "action_url", length = 500)
    private String actionUrl; // Deep link or URL for action

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> metadata; // Additional data as JSON
}
