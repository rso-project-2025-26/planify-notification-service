package com.planify.notification.controller;

import com.planify.notification.model.InAppNotification;
import com.planify.notification.repository.InAppNotificationRepository;
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
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "In-App Notifications", description = "User-facing endpoints for managing in-app notification feed and real-time updates")
@SecurityRequirement(name = "bearer-jwt")
public class InAppNotificationController {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Pridobimo vsa obvestila določenega uporabnika (paginated)
     */
    @GetMapping("/user/{userId}")
    @Operation(
        summary = "Get user's notifications",
        description = "Returns a paginated list of in-app notifications for the specified user, ordered by creation date descending."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved notifications",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = InAppNotification.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Page<InAppNotification>> getUserNotifications(
            @Parameter(required = true)
            @PathVariable UUID userId,
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable) {
        Page<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Pridobimo samo neprebrana sporočila uporabnika
     */
    @GetMapping("/user/{userId}/unread")
    @Operation(
        summary = "Get user's unread notifications",
        description = "Returns all unread in-app notifications for the specified user, ordered by creation date descending."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved unread notifications",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = InAppNotification.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<List<InAppNotification>> getUserUnreadNotifications(
            @Parameter(required = true)
            @PathVariable UUID userId) {
        List<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Pridobimo število neprebranih sporočil uporabnika
     */
    @GetMapping("/user/{userId}/unread/count")
    @Operation(
        summary = "Get unread notification count",
        description = "Returns the total number of unread notifications for the specified user. Used for notification badges."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved count",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Long> getUserUnreadNotificationCount(
            @Parameter(required = true)
            @PathVariable UUID userId) {
        long count = inAppNotificationRepository.countByUserIdAndIsReadFalse(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Označimo obvestilo kot prebrano
     */
    @PutMapping("/{notificationId}/read")
    @Operation(
        summary = "Mark notification as read",
        description = "Marks a single in-app notification as read. Sends real-time update via WebSocket to update the user's unread count."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully marked as read",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = InAppNotification.class))),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<InAppNotification> markAsRead(
            @Parameter(required = true)
            @PathVariable UUID notificationId) {
        return inAppNotificationRepository.findById(notificationId)
                .map(notification -> {
                    if (!notification.getIsRead()) {
                        notification.setIsRead(true);
                        notification.setReadAt(LocalDateTime.now());
                        InAppNotification updated = inAppNotificationRepository.save(notification);
                        
                        // Pošljemo posodobljeno število neprebranih obvestil uporabniku preko WebSocket-a
                        webSocketNotificationService.sendNotificationCountUpdate(notification.getUserId());
                        
                        return ResponseEntity.ok(updated);
                    }
                    return ResponseEntity.ok(notification);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Oznamčimo vsa obvestila uporabniku kot prebrana
     */
    @PutMapping("/user/{userId}/read-all")
    @Operation(
        summary = "Mark all notifications as read",
        description = "Marks all unread notifications for the specified user as read. Sends real-time update via WebSocket."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All notifications marked as read", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Void> markAllAsRead(
            @Parameter(required = true)
            @PathVariable UUID userId) {
        List<InAppNotification> unreadNotifications = inAppNotificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        
        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> {
            notification.setIsRead(true);
            notification.setReadAt(now);
        });
        
        inAppNotificationRepository.saveAll(unreadNotifications);
        
        // Pošljemo posodobljeno število neprebranih obvestil uporabniku preko WebSocket-a
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Izbrišemo obvestilo
     */
    @DeleteMapping("/{notificationId}")
    @Operation(
        summary = "Delete notification",
        description = "Permanently deletes a single in-app notification. Sends real-time update via WebSocket to update the user's unread count."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Notification successfully deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Void> deleteNotification(
            @Parameter(required = true)
            @PathVariable UUID notificationId) {
        if (!inAppNotificationRepository.existsById(notificationId)) {
            return ResponseEntity.notFound().build();
        }
        
        InAppNotification notification = inAppNotificationRepository.findById(notificationId).orElseThrow();
        UUID userId = notification.getUserId();
        
        inAppNotificationRepository.deleteById(notificationId);
        
        // Pošljemo posodobljeno število neprebranih obvestil uporabniku preko WebSocket-a
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Izbrišemo vsa obvestila nekega uporabnika
     */
    @DeleteMapping("/user/{userId}/all")
    @Operation(
        summary = "Delete all user notifications",
        description = "Permanently deletes all in-app notifications for the specified user. Sends real-time update via WebSocket."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "All notifications successfully deleted", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Void> deleteAllUserNotifications(
            @Parameter(required = true)
            @PathVariable UUID userId) {
        List<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        
        inAppNotificationRepository.deleteAll(notifications);
        
        // Pošljemo posodobljeno število neprebranih obvestil uporabniku preko WebSocket-a
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        return ResponseEntity.noContent().build();
    }
}
