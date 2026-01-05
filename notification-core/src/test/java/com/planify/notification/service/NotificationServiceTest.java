package com.planify.notification.service;

import com.planify.notification.event.*;
import com.planify.notification.model.EventAttendeeReminder;
import com.planify.notification.model.NotificationTemplate;
import com.planify.notification.model.NotificationType;
import com.planify.notification.repository.EventAttendeeReminderRepository;
import com.planify.notification.repository.NotificationLogRepository;
import com.planify.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private TemplateService templateService;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private EventAttendeeReminderRepository eventAttendeeReminderRepository;

    @Mock
    private UserDirectoryClient userDirectoryClient;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationTemplate template;
    private UUID eventId;
    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        template = new NotificationTemplate();
        template.setId(UUID.randomUUID());
        template.setTemplateKey("TEST_TEMPLATE");
        template.setType(NotificationType.EMAIL);
        template.setSubject("Test Subject");
        template.setBodyTemplate("Test Body {{userName}}");
        template.setIsActive(true);
    }

    @Test
    void handleEventAttendanceAcceptedEvent_shouldSaveNewAttendee() {
        // Given
        EventAttendanceAcceptedEvent event = new EventAttendanceAcceptedEvent();
        event.setEventId(eventId);
        event.setEventTitle("Test Event");
        event.setEventStartAt(OffsetDateTime.now().plusDays(1));
        event.setUserId(userId);

        when(eventAttendeeReminderRepository.findByEventIdAndUserId(eventId, userId))
            .thenReturn(Optional.empty());
        when(eventAttendeeReminderRepository.save(any(EventAttendeeReminder.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationService.handleEventAttendanceAcceptedEvent(event);

        // Then
        verify(eventAttendeeReminderRepository).findByEventIdAndUserId(eventId, userId);
        verify(eventAttendeeReminderRepository).save(any(EventAttendeeReminder.class));
    }

    @Test
    void handleEventAttendanceAcceptedEvent_shouldSkipIfAttendeeAlreadyExists() {
        // Given
        EventAttendanceAcceptedEvent event = new EventAttendanceAcceptedEvent();
        event.setEventId(eventId);
        event.setUserId(userId);

        EventAttendeeReminder existingAttendee = new EventAttendeeReminder();
        existingAttendee.setEventId(eventId);
        existingAttendee.setUserId(userId);

        when(eventAttendeeReminderRepository.findByEventIdAndUserId(eventId, userId))
            .thenReturn(Optional.of(existingAttendee));

        // When
        notificationService.handleEventAttendanceAcceptedEvent(event);

        // Then
        verify(eventAttendeeReminderRepository).findByEventIdAndUserId(eventId, userId);
        verify(eventAttendeeReminderRepository, never()).save(any());
    }

    @Test
    void handleJoinRequestSentEvent_shouldProcessRequest() {
        // Given
        JoinRequestsSentEvent event = new JoinRequestsSentEvent();
        event.setJoinRequestId(UUID.randomUUID());
        event.setRequesterUserId(userId);
        event.setOrganizationId(organizationId);
        event.setOrganizationName("Test Org");
        event.setRequesterUsername("testuser");
        event.setAdminIds(Arrays.asList(UUID.randomUUID().toString()));

        template.setTemplateKey("NEW_REQUEST");
        when(templateRepository.findByTemplateKey("NEW_REQUEST"))
            .thenReturn(Optional.of(template));

        // When
        notificationService.handleJoinRequestSentEvent(event);

        // Then
        verify(templateRepository).findByTemplateKey("NEW_REQUEST");
    }

    @Test
    void handleJoinRequestRespondedEvent_shouldSendApprovedNotificationWhenApproved() {
        // Given
        JoinRequestRespondedEvent event = new JoinRequestRespondedEvent();
        event.setJoinRequestId(UUID.randomUUID());
        event.setRequesterUserId(userId);
        event.setOrganizationId(organizationId);
        event.setOrganizationName("Test Org");
        event.setEventType("APPROVED");
        event.setRequesterFirstName("John");
        event.setRequesterLastName("Doe");
        event.setRequesterEmail("john@example.com");

        template.setTemplateKey("REQUEST_ACCEPTED");
        when(templateRepository.findByTemplateKey("REQUEST_ACCEPTED"))
            .thenReturn(Optional.of(template));

        // When
        notificationService.handleJoinRequestRespondedEvent(event);

        // Then
        verify(templateRepository).findByTemplateKey("REQUEST_ACCEPTED");
    }

    @Test
    void handleJoinRequestRespondedEvent_shouldSendRejectedNotificationWhenRejected() {
        // Given
        JoinRequestRespondedEvent event = new JoinRequestRespondedEvent();
        event.setJoinRequestId(UUID.randomUUID());
        event.setRequesterUserId(userId);
        event.setOrganizationId(organizationId);
        event.setOrganizationName("Test Org");
        event.setEventType("REJECTED");
        event.setRequesterFirstName("John");
        event.setRequesterLastName("Doe");
        event.setRequesterEmail("john@example.com");

        template.setTemplateKey("REQUEST_DECLINED");
        when(templateRepository.findByTemplateKey("REQUEST_DECLINED"))
            .thenReturn(Optional.of(template));

        // When
        notificationService.handleJoinRequestRespondedEvent(event);

        // Then
        verify(templateRepository).findByTemplateKey("REQUEST_DECLINED");
    }

    @Test
    void handleInvitationSentEvent_shouldProcessInvitation() {
        // Given
        InvitationSentEvent event = new InvitationSentEvent();
        event.setInvitationId(UUID.randomUUID());
        event.setInvitedUserId(userId);
        event.setOrganizationId(organizationId);
        event.setOrganizationName("Test Org");

        template.setTemplateKey("NEW_INVITATION");
        when(templateRepository.findByTemplateKey("NEW_INVITATION"))
            .thenReturn(Optional.of(template));

        // When
        notificationService.handleInvitationSentEvent(event);

        // Then
        verify(templateRepository).findByTemplateKey("NEW_INVITATION");
    }

    @Test
    void handleInvitationRespondedEvent_shouldHandleAcceptedInvitation() {
        // Given
        InvitationRespondedEvent event = new InvitationRespondedEvent();
        event.setInvitationId(UUID.randomUUID());
        event.setInvitedUserId(userId);
        event.setOrganizationId(organizationId);
        event.setOrganizationName("Test Org");
        event.setEventType("ACCEPTED");
        event.setInvitedUsername("JaneSmith");
        event.setAdminIds(Arrays.asList(UUID.randomUUID().toString()));

        template.setTemplateKey("INVITATION_ACCEPTED");
        when(templateRepository.findByTemplateKey("INVITATION_ACCEPTED"))
            .thenReturn(Optional.of(template));

        // When
        notificationService.handleInvitationRespondedEvent(event);

        // Then
        verify(templateRepository).findByTemplateKey("INVITATION_ACCEPTED");
    }

    @Test
    void handleInvitationRespondedEvent_shouldHandleDeclinedInvitation() {
        // Given
        InvitationRespondedEvent event = new InvitationRespondedEvent();
        event.setInvitationId(UUID.randomUUID());
        event.setInvitedUserId(userId);
        event.setOrganizationId(organizationId);
        event.setOrganizationName("Test Org");
        event.setEventType("DECLINED");
        event.setInvitedUsername("JaneSmith");
        event.setAdminIds(Arrays.asList(UUID.randomUUID().toString()));

        template.setTemplateKey("INVITATION_DECLINED");
        when(templateRepository.findByTemplateKey("INVITATION_DECLINED"))
            .thenReturn(Optional.of(template));

        // When
        notificationService.handleInvitationRespondedEvent(event);

        // Then
        verify(templateRepository).findByTemplateKey("INVITATION_DECLINED");
    }
}
