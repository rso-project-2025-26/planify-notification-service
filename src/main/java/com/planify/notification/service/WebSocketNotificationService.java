package com.planify.notification.service;

import com.planify.notification.dto.InAppNotificationDto;
import com.planify.notification.model.InAppNotification;
import com.planify.notification.repository.InAppNotificationRepository;
import com.planify.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final InAppNotificationRepository inAppNotificationRepository;
    private final NotificationWebSocketHandler webSocketHandler;

    /**
     * Pošlje in-app obvestilo in ga shrani v bazi
     */
    @Transactional
    public InAppNotification sendInAppNotification(
            UUID userId,
            String title,
            String message,
            String notificationType,
            UUID referenceId,
            String referenceType) {

        // Shranjevanje v plenify bazo
        InAppNotification notification = new InAppNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setNotificationType(notificationType);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setIsRead(false);

        InAppNotification saved = inAppNotificationRepository.save(notification);
        log.info("Saved in-app notification {} for user {}", saved.getId(), userId);

        // Preko WebSocket pošljemo obvesilo uporabniku
        try {
            InAppNotificationDto dto = convertToDto(saved);
            sendToUser(String.valueOf(userId), dto);
            log.info("Sent WebSocket notification to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification to user {}", userId, e);
        }

        return saved;
    }

    public void sendToUser(String userId, InAppNotificationDto notification) {
        webSocketHandler.sendNotificationToUser(userId, notification);
    }

    public void broadcast(InAppNotificationDto notification) {
        webSocketHandler.broadcastNotification(notification);
    }

    public boolean isUserOnline(String userId) {
        return webSocketHandler.isUserConnected(userId);
    }

    public int getActiveConnectionsCount() {
        return webSocketHandler.getActiveConnectionCount();
    }

    public void sendNotificationCountUpdate(UUID userId) {
        try {
            long unreadCount = inAppNotificationRepository.countByUserIdAndIsReadFalse(userId);

            // Ustvarimo objekt za posodabljanja števca obvestil
            Map<String, Object> countUpdate = new HashMap<>();
            countUpdate. put("unreadCount", unreadCount);
            countUpdate.put("userId", userId.toString());
            countUpdate.put("type", "NOTIFICATION_COUNT_UPDATE");

            // Pošljemo preko WebSocket-a
            webSocketHandler.sendNotificationToUser(userId.toString(), countUpdate);

            log.info("Sent notification count update to user {}: {}", userId, unreadCount);
        } catch (Exception e) {
            log.error("Failed to send notification count to user {}", userId, e);
        }
    }

    private InAppNotificationDto convertToDto(InAppNotification notification) {
        InAppNotificationDto dto = new InAppNotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setNotificationType(notification.getNotificationType());
        dto.setReferenceId(notification.getReferenceId());
        dto.setReferenceType(notification.getReferenceType());
        dto.setActionUrl(notification.getActionUrl());
        dto.setIsRead(notification.getIsRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
