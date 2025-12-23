package com.planify.notification.repository;

import com.planify.notification.model.EventAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventAttendeeRepository extends JpaRepository<EventAttendee, UUID> {

    Optional<EventAttendee> findByEventIdAndUserId(UUID eventId, UUID userId);

    @Query("SELECT ea FROM EventAttendee ea WHERE ea.eventStartAt >= :from AND ea.eventStartAt < :to")
    List<EventAttendee> findAllBetween(@Param("from") OffsetDateTime from,
                                       @Param("to") OffsetDateTime to);
}
