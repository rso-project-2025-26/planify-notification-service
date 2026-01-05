package com.planify.notification.service;

import com.planify.notification.dto.InAppNotificationDto;
import com.planify.notification.model.InAppNotification;
import com.planify.notification.repository.InAppNotificationRepository;
import com.planify.notification.websocket.NotificationWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;

    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    @InjectMocks
    private WebSocketNotificationService webSocketNotificationService;

    private UUID userId;
    private UUID referenceId;
    private InAppNotification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        referenceId = UUID.randomUUID();

        notification = new InAppNotification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setTitle("Test Notification");
        notification.setMessage("Test Message");
        notification.setNotificationType("test_type");
        notification.setReferenceId(referenceId);
        notification.setReferenceType("test_reference");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void sendInAppNotification_shouldSaveAndSendNotification() {
        // Given
        when(inAppNotificationRepository.save(any(InAppNotification.class)))
            .thenReturn(notification);

        // When
        InAppNotification result = webSocketNotificationService.sendInAppNotification(
            userId,
            "Test Notification",
            "Test Message",
            "test_type",
            referenceId,
            "test_reference"
        );

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals("Test Notification", result.getTitle());
        verify(inAppNotificationRepository).save(any(InAppNotification.class));
        verify(webSocketHandler).sendNotificationToUser(eq(userId.toString()), any(InAppNotificationDto.class));
    }

    @Test
    void sendInAppNotification_shouldHandleWebSocketFailure() {
        // Given
        when(inAppNotificationRepository.save(any(InAppNotification.class)))
            .thenReturn(notification);
        doThrow(new RuntimeException("WebSocket error"))
            .when(webSocketHandler).sendNotificationToUser(anyString(), any());

        // When
        InAppNotification result = webSocketNotificationService.sendInAppNotification(
            userId,
            "Test Notification",
            "Test Message",
            "test_type",
            referenceId,
            "test_reference"
        );

        // Then - notification should still be saved even if WebSocket fails
        assertNotNull(result);
        verify(inAppNotificationRepository).save(any(InAppNotification.class));
        verify(webSocketHandler).sendNotificationToUser(anyString(), any());
    }

    @Test
    void sendToUser_shouldCallWebSocketHandler() {
        // Given
        InAppNotificationDto dto = new InAppNotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(userId);

        // When
        webSocketNotificationService.sendToUser(userId.toString(), dto);

        // Then
        verify(webSocketHandler).sendNotificationToUser(userId.toString(), dto);
    }

    @Test
    void broadcast_shouldCallWebSocketHandler() {
        // Given
        InAppNotificationDto dto = new InAppNotificationDto();
        dto.setId(notification.getId());

        // When
        webSocketNotificationService.broadcast(dto);

        // Then
        verify(webSocketHandler).broadcastNotification(dto);
    }

    @Test
    void isUserOnline_shouldReturnWebSocketHandlerResult() {
        // Given
        when(webSocketHandler.isUserConnected(userId.toString())).thenReturn(true);

        // When
        boolean result = webSocketNotificationService.isUserOnline(userId.toString());

        // Then
        assertTrue(result);
        verify(webSocketHandler).isUserConnected(userId.toString());
    }

    @Test
    void getActiveConnectionsCount_shouldReturnCorrectCount() {
        // Given
        int expectedCount = 5;
        when(webSocketHandler.getActiveConnectionCount()).thenReturn(expectedCount);

        // When
        int result = webSocketNotificationService.getActiveConnectionsCount();

        // Then
        assertEquals(expectedCount, result);
        verify(webSocketHandler).getActiveConnectionCount();
    }

    @Test
    void sendNotificationCountUpdate_shouldSendCorrectCountToUser() {
        // Given
        long unreadCount = 10L;
        when(inAppNotificationRepository.countByUserIdAndIsReadFalse(userId))
            .thenReturn(unreadCount);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        // When
        webSocketNotificationService.sendNotificationCountUpdate(userId);

        // Then
        verify(inAppNotificationRepository).countByUserIdAndIsReadFalse(userId);
        verify(webSocketHandler).sendNotificationToUser(eq(userId.toString()), captor.capture());
        
        Map<String, Object> sentData = captor.getValue();
        assertEquals(unreadCount, sentData.get("unreadCount"));
        assertEquals(userId.toString(), sentData.get("userId"));
        assertEquals("NOTIFICATION_COUNT_UPDATE", sentData.get("type"));
    }

    @Test
    void sendNotificationCountUpdate_shouldHandleExceptions() {
        // Given
        when(inAppNotificationRepository.countByUserIdAndIsReadFalse(userId))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertDoesNotThrow(() -> webSocketNotificationService.sendNotificationCountUpdate(userId));
        verify(inAppNotificationRepository).countByUserIdAndIsReadFalse(userId);
    }
}
