package com.planify.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitationSentEvent {
    private UUID invitationId;
    private UUID organizationId;
    private String organizationName;
    private UUID invitedUserId;
    private String invitedFirstName;
    private String invitedLastName;
    private String invitedEmail;
    private Instant occurredAt;
}