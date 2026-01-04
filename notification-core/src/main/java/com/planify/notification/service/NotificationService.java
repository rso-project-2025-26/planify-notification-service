package com.planify.notification.service;

import com.planify.notification.event.*;
import com.planify.notification.model.*;
import com.planify.notification.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final TemplateService templateService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final EventAttendeeReminderRepository eventAttendeeReminderRepository;
    private final UserDirectoryClient userDirectoryClient;

    @Value("${notification.email.retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${notification.reminder.advance-notice-hours:24}")
    private int reminderAdvanceHours;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Transactional
    public void handleEventAttendanceAcceptedEvent(EventAttendanceAcceptedEvent event) {
        log.info("Handling event attendance accepted: event {} by user {} at {}", event.getEventId(), event.getUserId(), event.getEventStartAt());

        // Idempotentnost (eventId, userId)
        var existing = eventAttendeeReminderRepository.findByEventIdAndUserId(event.getEventId(), event.getUserId());
        if (existing.isPresent()) {
            log.info("EventAttendee already exists for event {} and user {} — skipping insert", event.getEventId(), event.getUserId());
            return;
        }

        EventAttendeeReminder attendee = new EventAttendeeReminder();
        attendee.setEventId(event.getEventId());
        attendee.setEventTitle(event.getEventTitle());
        attendee.setEventStartAt(event.getEventStartAt());
        attendee.setUserId(event.getUserId());

        eventAttendeeReminderRepository.save(attendee);
        log.info("Stored attendee for event {} and user {}", event.getEventId(), event.getUserId());
    }

    @Transactional
    public void handleJoinRequestSentEvent(JoinRequestsSentEvent event) {
        log.info("Handling join request {} from user {} to organization {}",
                event.getJoinRequestId(), event.getRequesterUserId(), event.getOrganizationId());
        sendJoinRequestSentNotification(event);
    }

    @Transactional
    public void handleJoinRequestRespondedEvent(JoinRequestRespondedEvent event) {
        log.info("Handling responded join request {} from user {} to organization {}",
                event.getJoinRequestId(), event.getRequesterUserId(), event.getOrganizationId());

        String eventType = event.getEventType();

        if ("APPROVED".equals(eventType)) {
            sendJoinRequestApprovedNotification(event);
        } else if ("REJECTED".equals(eventType)) {
            sendJoinRequestRejectedNotification(event);
        }
    }


    @Transactional
    public void handleInvitationSentEvent(InvitationSentEvent event) {
        log.info("Handling invitation {} for user {} from organization {}",
                event.getInvitationId(), event.getInvitedUserId(), event.getOrganizationId());

        sendInvitationNotification(event);
    }

    @Transactional
    public void handleInvitationRespondedEvent(InvitationRespondedEvent event) {
        log.info("Handling responded invitation {} for user {} from organization {}",
                event.getInvitationId(), event.getInvitedUserId(), event.getOrganizationId());

        String eventType = event.getEventType();
        if ("ACCEPTED".equals(eventType)) {
            sendInvitationAcceptedNotification(event);
        } else if ("DECLINED".equals(eventType)) {
            sendInvitationDeclinedNotification(event);
        }
    }

    private void sendJoinRequestSentNotification(JoinRequestsSentEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("NEW_REQUEST");
        if (templateOpt.isEmpty()) {
            log.error("NEW_REQUEST template not found");
            return;
        }

        NotificationTemplate template = templateOpt.get();

        Map<String, Object> variables = new HashMap<>();
        variables.put("adminName", event.getOrganizationName() + "'s Admin");
        variables.put("userName", event.getRequesterUsername());
        variables.put("orgName", event.getOrganizationName());
        variables.put("requestReviewLink", "/organizations/admin");

        List<String> adminIds = event.getAdminIds();
        for (String adminId : adminIds) {
            // Pošljemo obvestilo adminu organizacije
            sendNotification(
                    null,
                    UUID.fromString(adminId),
                    null, // Ne pošiljamo email obvestil
                    null, // Ne pošiljamo SMS obvestil
                    template,
                    variables,
                    "join_request_accepted",
                    event.getJoinRequestId(),
                    "join_request");
        }

        log.info("Sent NEW_REQUEST notification to admins of organization {}", event.getOrganizationId());
    }

    private void sendJoinRequestApprovedNotification(JoinRequestRespondedEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("REQUEST_ACCEPTED");
        if (templateOpt.isEmpty()) {
            log.error("REQUEST_ACCEPTED template not found");
            return;
        }

        NotificationTemplate template = templateOpt.get();

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getRequesterFirstName() + " " + event.getRequesterLastName());
        variables.put("orgName", event.getOrganizationName());

        // Pošljemo obvestilo uporabniku
        sendNotification(
                null,
                event.getRequesterUserId(),
                event.getRequesterEmail(),
                null, // Ne pošiljamo SMS obvestil
                template,
                variables,
                "join_request_approved",
                event.getJoinRequestId(),
                "join_request"
        );
    }

    private void sendJoinRequestRejectedNotification(JoinRequestRespondedEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("REQUEST_DECLINED");
        if (templateOpt.isEmpty()) {
            log.error("REQUEST_DECLINED template not found");
            return;
        }

        NotificationTemplate template = templateOpt.get();

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", event.getRequesterFirstName() + " " + event.getRequesterLastName());
        variables.put("orgName", event.getOrganizationName());

        // Pošljemo obvestilo uporabniku
        sendNotification(
                null,
                event.getRequesterUserId(),
                event.getRequesterEmail(),
                null, // Ne pošiljamo SMS obvestil
                template,
                variables,
                "join_request_rejected",
                event.getJoinRequestId(),
                "join_request"
        );
    }

    private void sendInvitationNotification(InvitationSentEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("NEW_INVITATION");
        if (templateOpt.isEmpty()) {
            log.error("NEW_INVITATION template not found");
            return;
        }

        NotificationTemplate template = templateOpt.get();

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", event.getInvitedFirstName() + " " + event.getInvitedLastName());
        variables.put("orgName", event.getOrganizationName());
        variables.put("invitatioAcceptLink", "/organizations/my");

        // Pošljemo obvestilo povabljenemu uporabniku
        sendNotification(
                null,
                event.getInvitedUserId(),
                event.getInvitedEmail(),
                null, // Ne pošiljamo SMS obvestil
                template,
                variables,
                "invitation_received",
                event.getInvitationId(),
                "invitation"
        );
    }

    private void sendInvitationAcceptedNotification(InvitationRespondedEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("INVITATION_ACCEPTED");
        if (templateOpt.isEmpty()) {
            log.error("INVITATION_ACCEPTED template not found");
            return;
        }

        NotificationTemplate template = templateOpt.get();

        Map<String, Object> variables = new HashMap<>();
        variables.put("adminName", event.getOrganizationName() + "'s Admin");
        variables.put("userName", event.getInvitedUsername());
        variables.put("orgName", event.getOrganizationName());
        variables.put("memberListLink", "/organizations/admin");

        List<String> adminIds = event.getAdminIds();
        for (String adminId : adminIds) {
            // Pošljemo obvestilo adminu organizacije
            sendNotification(
                    null,
                    UUID.fromString(adminId),
                    null, // Ne pošiljamo email obvestil
                    null, // Ne pošiljamo SMS obvestil
                    template,
                    variables,
                    "invitation_accepted",
                    event.getInvitationId(),
                    "invitation");
        }

        log.info("Sent INVITATION_ACCEPTED notification to admins of organization {}", event.getOrganizationId());
    }

    private void sendInvitationDeclinedNotification(InvitationRespondedEvent event) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("INVITATION_DECLINED");
        if (templateOpt.isEmpty()) {
            log.error("INVITATION_DECLINED template not found");
            return;
        }

        NotificationTemplate template = templateOpt.get();

        Map<String, Object> variables = new HashMap<>();
        variables.put("adminName", event.getOrganizationName() + "'s Admin");
        variables.put("userName", event.getInvitedUsername());
        variables.put("orgName", event.getOrganizationName());

        List<String> adminIds = event.getAdminIds();
        for (String adminId : adminIds) {
            // Pošljemo obvestilo adminu organizacije
            sendNotification(
                    null,
                    UUID.fromString(adminId),
                    null, // Ne pošiljamo email obvestil
                    null, // Ne pošiljamo SMS obvestil
                    template,
                    variables,
                    "invitation_declined",
                    event.getInvitationId(),
                    "invitation");
        }

        log.info("Sent INVITATION_DECLINED notification to admins of organization {}", event.getOrganizationId());
    }

    private void sendNotification(
            UUID eventId,
            UUID userId,
            String email,
            String phone,
            NotificationTemplate template,
            Map<String, Object> variables,
            String notificationType,
            UUID referenceId,
            String referenceType) {
        NotificationLog log = new NotificationLog();
        log.setEventId(eventId);
        log.setUserId(userId);
        log.setRecipientEmail(email);
        log.setRecipientPhone(phone);
        log.setType(template.getType());
        log.setTemplateKey(template.getTemplateKey());
        log.setStatus(NotificationStatus.PENDING);

        boolean sentSuccessfully = false;

        try {
            String subject = templateService.renderTemplate(template.getSubject(), variables);
            String body = templateService.renderTemplate(template.getBodyTemplate(), variables);

            log.setSubject(subject);
            log.setBody(body);

            // Pošljemo obvestilo v aplikaciji z uporabo WebSocket (za APP, EMAIL_APP, ali ALL tip)
            if (userId != null &&
                    (template.getType() == NotificationType.APP ||
                            template.getType() == NotificationType.EMAIL_APP ||
                            template.getType() == NotificationType.ALL)) {
                try {
                    webSocketNotificationService.sendInAppNotification(
                            userId,
                            subject,
                            body,
                            notificationType,
                            referenceId,
                            referenceType
                    );
                    sentSuccessfully = true;
                    this.log.info("Sent in-app notification to user {}", userId);
                } catch (Exception e) {
                    this.log.error("Failed to send in-app notification to user {}", userId, e);
                }
            }

            // Pošlji email (za EMAIL, EMAIL_APP, ali ALL tip)
            if ((template.getType() == NotificationType.EMAIL ||
                    template.getType() == NotificationType.EMAIL_APP ||
                    template.getType() == NotificationType.ALL) && email != null) { // email uporabnika imamo le če je ta privolil k uporabi
                try {
                    String externalId = emailService.sendEmail(email, subject, body);
                    log.setExternalId(externalId);
                    sentSuccessfully = true;
                    this.log.info("Sent email to {}", email);
                } catch (Exception e) {
                    this.log.error("Failed to send email to {}", email, e);
                }
            }

            // Pošiljanje SMS (za SMS ali ALL tip)
            if ((template.getType() == NotificationType.SMS || template.getType() == NotificationType.ALL)
                    && phone != null && template.getSmsTemplate() != null) { // tel. št. uporabnika pošljemo le če je ta privolil k uporabi
                try {
                    String smsBody = templateService.renderSmsTemplate(template.getSmsTemplate(), variables);
                    String smsSid = smsService.sendSms(phone, smsBody, 160);

                    if (log.getExternalId() == null) {
                        log.setExternalId(smsSid);
                    }
                    sentSuccessfully = true;
                    this.log.info("Sent SMS to {}", phone);
                } catch (Exception e) {
                    this.log.error("Failed to send SMS to {}", phone, e);
                }
            }

            if (sentSuccessfully) {
                log.setStatus(NotificationStatus.SENT);
                log.setSentAt(LocalDateTime.now());
            } else {
                log.setStatus(NotificationStatus.FAILED);
                log.setErrorMessage("No notification channels were successful");
            }

        } catch (Exception e) {
            this.log.error("Failed to send notification", e);
            log.setStatus(NotificationStatus.FAILED);
            log.setErrorMessage(e.getMessage());
        }

        logRepository.save(log);
    }

    @Transactional
    public int sendScheduledReminders() {
        // Izračunamo okno, ki predstavlja jutri po UTC [00:00, 24:00)
        var now = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC);
        var tomorrowStart = now.plusDays(1).toLocalDate().atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        var tomorrowEnd = tomorrowStart.plusDays(1);

        var attendees = eventAttendeeReminderRepository.findAllBetween(tomorrowStart, tomorrowEnd);
        log.info("Found {} attendees with events tomorrow ({} to {})", attendees.size(), tomorrowStart, tomorrowEnd);

        int remindersSent = 0;
        for (EventAttendeeReminder attendee : attendees) {
            NotificationLog notificationLog = new NotificationLog();
            notificationLog.setEventId(attendee.getEventId());
            notificationLog.setUserId(attendee.getUserId());
            notificationLog.setType(NotificationType.SMS);
            notificationLog.setTemplateKey("EVENT_REMINDER_SMS");
            notificationLog.setSubject("Event Reminder");

            UserDirectoryClient.UserResponse user = userDirectoryClient.getUser(attendee.getUserId());
            if (user == null) {
                log.error("User {} not found", attendee.getUserId());
                continue;
            }
            try {
                if (attendee.getIsSent()) {
                    log.info("Event {} reminder already sent to user {}", attendee.getEventId(), attendee.getUserId());
                    continue;
                }
                Boolean smsConsent = user.getSmsConsent();
                if (smsConsent == null || !smsConsent) {
                    log.info("User {} has not given SMS consent, skipping SMS reminder for event {}", attendee.getUserId(), attendee.getEventId());
                    continue;
                }
                String phone = user.getPhoneNumber();
                if (phone == null || phone.isBlank()) {
                    log.info("No phone for user {}, skipping SMS reminder for event {}", attendee.getUserId(), attendee.getEventId());
                    continue;
                }

                String when = attendee.getEventStartAt().atZoneSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime().format(DATE_FORMATTER);
                String body = String.format("You have an event coming tomorrow! Event: %s Start time: %s", attendee.getEventTitle(), when);
                notificationLog.setBody(body);

                String smsSid = smsService.sendSms(phone, body, 160);

                notificationLog.setStatus(NotificationStatus.SENT);
                notificationLog.setSentAt(LocalDateTime.now());
                notificationLog.setExternalId(smsSid);
                logRepository.save(notificationLog);

                attendee.setIsSent(true);
                attendee.setSentAt(OffsetDateTime.now(ZoneOffset.UTC));
                eventAttendeeReminderRepository.save(attendee);
                remindersSent++;
            } catch (Exception ex) {
                notificationLog.setStatus(NotificationStatus.FAILED);
                notificationLog.setErrorMessage(ex.getMessage());
                logRepository.save(notificationLog);
                log.error("Failed to send reminder for event {} to user {}", attendee.getEventId(), attendee.getUserId(), ex);

                log.info("Trying to send email and in-app notification for user {}", attendee.getUserId());
                Boolean emailConsent = user.getEmailConsent();
                if (emailConsent == null || !emailConsent) {
                    log.info("User {} has not given email consent, skipping email reminder for event {}", attendee.getUserId(), attendee.getEventId());
                    continue;
                }
                String email = user.getEmail();
                if (email == null || email.isBlank()) {
                    log.info("No email for user {}, skipping email reminder for event {}", attendee.getUserId(), attendee.getEventId());
                    continue;
                }

                Optional<NotificationTemplate> templateOpt = templateRepository.findByTemplateKey("SMS_REMINDER_FALLBACK");
                if (templateOpt.isEmpty()) {
                    log.error("SMS_REMINDER_FALLBACK template not found");
                    continue;
                }

                NotificationTemplate template = templateOpt.get();

                Map<String, Object> variables = new HashMap<>();
                variables.put("event_title", attendee.getEventTitle());
                variables.put("event_start_at", attendee.getEventStartAt().format(DATE_FORMATTER));

                sendNotification(attendee.getEventId(), attendee.getUserId(), email, null, template, variables, "sms_reminder_fallback", attendee.getEventId(), "event");
            }
        }
        return remindersSent;
    }
}