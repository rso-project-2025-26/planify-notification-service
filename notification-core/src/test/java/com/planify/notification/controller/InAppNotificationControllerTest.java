package com.planify.notification.controller;

import com.planify.notification.model.InAppNotification;
import com.planify.notification.repository.InAppNotificationRepository;
import com.planify.notification.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InAppNotificationControllerTest {

    @Mock
    private InAppNotificationRepository inAppNotificationRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private InAppNotificationController controller;

    private UUID userId;
    private UUID notificationId;
    private InAppNotification notification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        
        notification = new InAppNotification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setTitle("Test Notification");
        notification.setMessage("Test message");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getUserNotifications_shouldReturnPagedNotifications() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<InAppNotification> notifications = Arrays.asList(notification);
        Page<InAppNotification> page = new PageImpl<>(notifications, pageable, 1);
        
        when(inAppNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable))
            .thenReturn(page);

        // When
        ResponseEntity<Page<InAppNotification>> response = controller.getUserNotifications(userId, pageable);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(inAppNotificationRepository).findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Test
    void getUserUnreadNotifications_shouldReturnUnreadOnly() {
        // Given
        List<InAppNotification> unreadNotifications = Arrays.asList(notification);
        when(inAppNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId))
            .thenReturn(unreadNotifications);

        // When
        ResponseEntity<List<InAppNotification>> response = controller.getUserUnreadNotifications(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(inAppNotificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Test
    void getUserUnreadNotificationCount_shouldReturnCorrectCount() {
        // Given
        long expectedCount = 5L;
        when(inAppNotificationRepository.countByUserIdAndIsReadFalse(userId))
            .thenReturn(expectedCount);

        // When
        ResponseEntity<Long> response = controller.getUserUnreadNotificationCount(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedCount, response.getBody());
        verify(inAppNotificationRepository).countByUserIdAndIsReadFalse(userId);
    }

    @Test
    void markAsRead_shouldUpdateNotificationAndSendWebSocketUpdate() {
        // Given
        when(inAppNotificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));
        when(inAppNotificationRepository.save(any(InAppNotification.class)))
            .thenReturn(notification);

        // When
        ResponseEntity<InAppNotification> response = controller.markAsRead(notificationId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(notification.getIsRead());
        assertNotNull(notification.getReadAt());
        verify(inAppNotificationRepository).save(notification);
        verify(webSocketNotificationService).sendNotificationCountUpdate(userId);
    }

    @Test
    void markAsRead_shouldReturnNotFoundWhenNotificationDoesNotExist() {
        // Given
        when(inAppNotificationRepository.findById(notificationId))
            .thenReturn(Optional.empty());

        // When
        ResponseEntity<InAppNotification> response = controller.markAsRead(notificationId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(inAppNotificationRepository, never()).save(any());
        verify(webSocketNotificationService, never()).sendNotificationCountUpdate(any());
    }

    @Test
    void markAllAsRead_shouldUpdateAllUnreadNotifications() {
        // Given
        InAppNotification notification2 = new InAppNotification();
        notification2.setId(UUID.randomUUID());
        notification2.setUserId(userId);
        notification2.setIsRead(false);
        
        List<InAppNotification> unreadNotifications = Arrays.asList(notification, notification2);
        when(inAppNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId))
            .thenReturn(unreadNotifications);

        // When
        ResponseEntity<Void> response = controller.markAllAsRead(userId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(notification.getIsRead());
        assertTrue(notification2.getIsRead());
        verify(inAppNotificationRepository).saveAll(unreadNotifications);
        verify(webSocketNotificationService).sendNotificationCountUpdate(userId);
    }

    @Test
    void deleteNotification_shouldDeleteAndSendWebSocketUpdate() {
        // Given
        when(inAppNotificationRepository.existsById(notificationId))
            .thenReturn(true);
        when(inAppNotificationRepository.findById(notificationId))
            .thenReturn(Optional.of(notification));

        // When
        ResponseEntity<Void> response = controller.deleteNotification(notificationId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(inAppNotificationRepository).deleteById(notificationId);
        verify(webSocketNotificationService).sendNotificationCountUpdate(userId);
    }

    @Test
    void deleteNotification_shouldReturnNotFoundWhenNotificationDoesNotExist() {
        // Given
        when(inAppNotificationRepository.existsById(notificationId))
            .thenReturn(false);

        // When
        ResponseEntity<Void> response = controller.deleteNotification(notificationId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(inAppNotificationRepository, never()).deleteById(any());
        verify(webSocketNotificationService, never()).sendNotificationCountUpdate(any());
    }
}
