package com.planify.notification.event;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event, ki se porži, ko uporabnik potrdi prisotnost na nekem dogodku
 */
@Data
public class EventAttendanceAcceptedEvent {
    private UUID eventId;
    private String eventTitle;
    private OffsetDateTime eventStartAt; // pričakuje UTC
    private UUID userId;
}
