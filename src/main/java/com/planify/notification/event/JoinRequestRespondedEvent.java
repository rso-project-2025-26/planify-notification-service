package com.planify.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestRespondedEvent {
    private String eventType;              // APPROVED / REJECTED
    private UUID joinRequestId;
    private UUID organizationId;
    private String organizationName;
    private UUID requesterUserId;
    private String requesterFirstName;
    private String requesterLastName;
    private String requesterEmail;
    private Instant occurredAt;
}