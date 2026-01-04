package com.planify.notification.kafka;

import com.planify.notification.event.InvitationRespondedEvent;
import com.planify.notification.event.InvitationSentEvent;
import com.planify.notification.event.JoinRequestRespondedEvent;
import com.planify.notification.event.JoinRequestsSentEvent;
import com.planify.notification.event.EventAttendanceAcceptedEvent;
import com.planify.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${kafka.topics.join-request-sent}",
        groupId = "${spring.kafka.consumer.group-id}",
        errorHandler = "kafkaListenerErrorHandler",
        containerFactory = "joinRequestsSentKafkaListenerContainerFactory"
    )
    public void handleJoinRequestSent(JoinRequestsSentEvent joinRequestEvent) {
        log.info("Received join request: {}, from user {} to organization {}", joinRequestEvent.getJoinRequestId(), joinRequestEvent.getRequesterUserId(), joinRequestEvent.getOrganizationName());
        try {
            notificationService.handleJoinRequestSentEvent(joinRequestEvent);
        } catch (Exception e) {
            log.error("Error handling join request {}", joinRequestEvent.getJoinRequestId(), e);
        }
    }
    @KafkaListener(
            topics = "${kafka.topics.join-request-responded}",
            groupId = "${spring.kafka.consumer.group-id}",
            errorHandler = "kafkaListenerErrorHandler",
            containerFactory = "joinRequestsRespondedKafkaListenerContainerFactory"
    )
    public void handleJoinRequestResponded(JoinRequestRespondedEvent joinRequestEvent) {
        log.info("Received join request: {}, from user {} to organization {}", joinRequestEvent.getJoinRequestId(), joinRequestEvent.getRequesterUserId(), joinRequestEvent.getOrganizationName());
        try {
            notificationService.handleJoinRequestRespondedEvent(joinRequestEvent);
        } catch (Exception e) {
            log.error("Error handling join request {}", joinRequestEvent.getJoinRequestId(), e);
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.invitation-sent}",
        groupId = "${spring.kafka.consumer.group-id}",
        errorHandler = "kafkaListenerErrorHandler",
        containerFactory = "invitationsSentKafkaListenerContainerFactory"
    )
    public void handleInvitationSent(InvitationSentEvent invitationEvent) {
        log.info("User {} invited to organization {} (invitationId: {})",
                invitationEvent.getInvitedUserId(), invitationEvent.getOrganizationId(), invitationEvent.getInvitationId());
        try {
            notificationService.handleInvitationSentEvent(invitationEvent);
        } catch (Exception e) {
            log.error("Error handling invitation {}", invitationEvent.getInvitationId(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.invitation-responded}",
            groupId = "${spring.kafka.consumer.group-id}",
            errorHandler = "kafkaListenerErrorHandler",
            containerFactory = "invitationsRespondedKafkaListenerContainerFactory"
    )
    public void handleInvitationResponded(InvitationRespondedEvent invitationEvent) {
        log.info("User {} invited to organization {} (invitationId: {})",
                invitationEvent.getInvitedUserId(), invitationEvent.getOrganizationId(), invitationEvent.getInvitationId());
        try {
            notificationService.handleInvitationRespondedEvent(invitationEvent);
        } catch (Exception e) {
            log.error("Error handling invitation {}", invitationEvent.getInvitationId(), e);
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.event-attendance-accepted}",
            groupId = "${spring.kafka.consumer.group-id}",
            errorHandler = "kafkaListenerErrorHandler",
            containerFactory = "eventAttendanceAcceptedKafkaListenerContainerFactory"
    )
    public void handleEventAttendanceAccepted(EventAttendanceAcceptedEvent event) {
        log.info("User {} accepted attendance for event {} starting at {}", event.getUserId(), event.getEventId(), event.getEventStartAt());
        try {
            notificationService.handleEventAttendanceAcceptedEvent(event);
        } catch (Exception e) {
            log.error("Error handling event attendance accepted for event {} and user {}", event.getEventId(), event.getUserId(), e);
        }
    }
}
