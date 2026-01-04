package com.planify.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_attendee_reminders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_attendee_event_user", columnNames = {"event_id", "user_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendeeReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_title", nullable = false)
    private String eventTitle;

    @Column(name = "event_start_at", nullable = false)
    private OffsetDateTime eventStartAt; // shranjeno kot timestamp

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @OneToOne
    @JoinColumn(name = "notification_log_id")
    private NotificationLog notificationLog;
}
