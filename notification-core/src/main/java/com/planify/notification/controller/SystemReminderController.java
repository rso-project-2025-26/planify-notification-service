package com.planify.notification.controller;

import com.planify.notification.model.InAppNotification;
import com.planify.notification.repository.InAppNotificationRepository;
import com.planify.notification.service.NotificationService;
import com.planify.notification.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reminders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Reminders", description = "Endpont for Azur (serverless function) for sending the reminders")
public class SystemReminderController {


    private final NotificationService notificationService;

    /**
     * Pošljemo opomnike za dogodke
     */
    @GetMapping("send")
    @Operation(
            summary = "Send reminders to all users that will attend any event tomorrow",
            description = "Send SMS reminders to all users that will attend any event tomorrowusing Vonage API "
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reminders successfully sent.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = InAppNotification.class))),
            @ApiResponse(responseCode = "500", description = "Some error happened during sending reminders", content = @Content),
    })
    public ResponseEntity<?> sendReminders() {
        try {
            Integer sent = notificationService.sendScheduledReminders();
            return ResponseEntity.ok(sent);
        } catch (Exception e) {
            log.error("Error during sending the remindes: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error during sending the remindes: " + e.getMessage());
        }
    }
}
