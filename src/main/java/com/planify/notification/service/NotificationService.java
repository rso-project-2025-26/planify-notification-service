package com.planify.notification.service;

import com.planify.notification.event.*;
import com.planify.notification.model.*;
import com.planify.notification.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final ScheduledReminderRepository reminderRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final TemplateService templateService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Value("${notification.email.retry-attempts:3}")
    private int maxRetryAttempts;

    @Value("${notification.reminder.advance-notice-hours:24}")
    private int reminderAdvanceHours;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

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

    // TODO implementiraj ustvarjanje opomnikov ob dodajanju gosta na dogodek
//    private void createScheduledReminder(GuestAddedEvent event) {
//        LocalDateTime reminderTime = event.getEventStartTime().minusHours(reminderAdvanceHours);
//
//        if (reminderTime.isBefore(LocalDateTime.now())) {
//            log.info("Event {} is too soon for reminder, skipping", event.getEventId());
//            return;
//        }
//
//        ScheduledReminder reminder = new ScheduledReminder();
//        reminder.setEventId(event.getEventId());
//        reminder.setEventName(event.getEventName());
//        reminder.setEventStartTime(event.getEventStartTime());
//        reminder.setRecipientEmail(event.getGuestEmail());
//        reminder.setRecipientPhone(event.getGuestPhone());
//        reminder.setRecipientName(event.getGuestName());
//        reminder.setReminderTime(reminderTime);
//        reminder.setHoursBeforeEvent(reminderAdvanceHours);
//        reminder.setType(NotificationType.ALL);
//        reminder.setIsSent(false);
//
//        reminderRepository.save(reminder);
//        log.info("Created scheduled reminder for event {} at {}", event.getEventId(), reminderTime);
//    }

//    @Transactional
//    public void sendScheduledReminders() {
//        LocalDateTime now = LocalDateTime.now();
//        var dueReminders = reminderRepository.findDueReminders(now);
//
//        log.info("Found {} due reminders to send", dueReminders.size());
//
//        for (ScheduledReminder reminder : dueReminders) {
//            try {
//                sendReminder(reminder);
//                reminder.setIsSent(true);
//                reminder.setSentAt(LocalDateTime.now());
//                reminderRepository.save(reminder);
//            } catch (Exception e) {
//                log.error("Failed to send reminder {}", reminder.getId(), e);
//            }
//        }
//    }

//    private void sendReminder(ScheduledReminder reminder) {
//        Optional<NotificationTemplate> templateOpt = templateRepository
//                .findByTemplateKeyAndIsActiveTrue("EVENT_REMINDER");
//
//        if (templateOpt.isEmpty()) {
//            log.error("EVENT_REMINDER template not found");
//            return;
//        }
//
//        NotificationTemplate template = templateOpt.get();
//
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("recipientName", reminder.getRecipientName());
//        variables.put("eventName", reminder.getEventName());
//        variables.put("eventDate", reminder.getEventStartTime().format(DATE_FORMATTER));
//        variables.put("eventLocation", ""); // Location not stored in reminder
//
//        sendNotification(
//                reminder.getEventId(),
//                null,
//                reminder.getRecipientEmail(),
//                reminder.getRecipientPhone(),
//                template,
//                variables);
//
//        log.info("Sent reminder for event {} to {}", reminder.getEventId(), reminder.getRecipientEmail());
//    }
}
