package com.planify.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InvitationRespondedEvent {
    @JsonProperty("eventType")
    private String eventType;              // DECLINED / ACCEPTED

    @JsonProperty("invitationId")
    private UUID invitationId;

    @JsonProperty("adminIds")
    @JsonDeserialize(contentAs = String.class)
    private List<String> adminIds;

    @JsonProperty("organizationId")
    private UUID organizationId;

    @JsonProperty("organizationName")
    private String organizationName;

    @JsonProperty("invitedUserId")
    private UUID invitedUserId;

    @JsonProperty("invitedUsername")
    private String invitedUsername;

    @JsonProperty("occurredAt")
    private Instant occurredAt;
}