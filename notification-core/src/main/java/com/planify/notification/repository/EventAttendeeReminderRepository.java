package com.planify.notification.repository;

import com.planify.notification.model.EventAttendeeReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAttendeeReminderRepository extends JpaRepository<EventAttendeeReminder, UUID> {

    Optional<EventAttendeeReminder> findByEventIdAndUserId(UUID eventId, UUID userId);

    @Query("SELECT ea FROM EventAttendeeReminder ea WHERE ea.eventStartAt >= :from AND ea.eventStartAt < :to")
    List<EventAttendeeReminder> findAllBetween(@Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to);
}
