package com.planify.notification.repository;

import com.planify.notification.model.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByTemplateKey(String templateKey);

    Optional<NotificationTemplate> findByTemplateKeyAndIsActiveTrue(String templateKey);
}
