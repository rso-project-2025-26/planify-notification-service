package com.planify.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestsSentEvent {
    private UUID joinRequestId;
    private List<String> adminIds;
    private UUID organizationId;
    private String organizationName;
    private UUID requesterUserId;
    private String requesterUsername;
    private Instant occurredAt;
}