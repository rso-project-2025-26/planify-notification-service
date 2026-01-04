package com.planify.notification.repository;

import com.planify.notification.model.NotificationLog;
import com.planify.notification.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByEventId(UUID eventId);

    List<NotificationLog> findByUserId(UUID userId);

    List<NotificationLog> findByStatus(NotificationStatus status);

    List<NotificationLog> findByStatusAndRetryCountLessThan(NotificationStatus status, Integer maxRetries);
}
