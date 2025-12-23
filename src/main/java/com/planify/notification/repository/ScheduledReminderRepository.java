package com.planify.notification.repository;

import com.planify.notification.model.ScheduledReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledReminderRepository extends JpaRepository<ScheduledReminder, UUID> {

    List<ScheduledReminder> findByEventId(UUID eventId);

    List<ScheduledReminder> findByIsSentFalse();

    @Query("SELECT r FROM ScheduledReminder r WHERE r.isSent = false AND r.reminderTime <= :now")
    List<ScheduledReminder> findDueReminders(@Param("now") LocalDateTime now);

    List<ScheduledReminder> findByEventIdAndIsSentFalse(UUID eventId);
}
