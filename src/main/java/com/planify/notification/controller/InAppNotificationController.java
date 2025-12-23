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
@Tag(name = "In-App Notifications", description = "Endpoints for managing user in-app notifications")
public class InAppNotificationController {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    /**
     * Pridobimo vsa obvestila določenega uporabnika (paginated)
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "List user's notifications (paginated)", description = "Returns a page of in-app notifications for the specified user.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications fetched",
                    content = @Content(schema = @Schema(implementation = InAppNotification.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Page<InAppNotification>> getUserNotifications(
            @PathVariable UUID userId,
            Pageable pageable) {
        Page<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Pridobimo samo neprebrana sporočila uporabnika
     */
    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "List user's unread notifications", description = "Returns all unread in-app notifications for the specified user.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread notifications fetched"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<List<InAppNotification>> getUserUnreadNotifications(
            @PathVariable UUID userId) {
        List<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Pridobimo število neprebranih sporočil uporabnika
     */
    @GetMapping("/user/{userId}/unread/count")
    @Operation(summary = "Get user's unread notification count", description = "Returns the number of unread notifications for the specified user.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Long> getUserUnreadNotificationCount(
            @PathVariable UUID userId) {
        long count = inAppNotificationRepository.countByUserIdAndIsReadFalse(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Označimo obvestilo kot prebrano
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read", description = "Marks a single in-app notification as read and updates the user's unread count.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification updated"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<InAppNotification> markAsRead(
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
    @Operation(summary = "Mark all user's notifications as read", description = "Marks all unread in-app notifications for the specified user as read.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Void> markAllAsRead(@PathVariable UUID userId) {
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
    @Operation(summary = "Delete a notification", description = "Deletes a single in-app notification.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Void> deleteNotification(
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
    @Operation(summary = "Delete all user's notifications", description = "Deletes all in-app notifications for the specified user.", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted")
    })
    @PreAuthorize("hasAnyRole('UPORABNIK','ADMINISTRATOR')")
    public ResponseEntity<Void> deleteAllUserNotifications(@Parameter(description = "User ID") @PathVariable UUID userId) {
        List<InAppNotification> notifications = inAppNotificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
        
        inAppNotificationRepository.deleteAll(notifications);
        
        // Pošljemo posodobljeno število neprebranih obvestil uporabniku preko WebSocket-a
        webSocketNotificationService.sendNotificationCountUpdate(userId);
        
        return ResponseEntity.noContent().build();
    }
}
