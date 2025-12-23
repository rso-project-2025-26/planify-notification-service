package com.planify.notification.scheduler;

import com.planify.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Spring Boot scheduled task for reminder checks
 * Alternative to AWS Lambda for local/cloud deployment
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScheduler {

    private final NotificationService notificationService;

    @Value("${notification.reminder.check-interval-minutes:30}")
    private int checkIntervalMinutes;

    // Izvede se na določen iterval
    @Scheduled(fixedRateString = "${notification.reminder.check-interval-minutes:30}00000")
    public void checkAndSendReminders() {
        log.info("Running scheduled reminder check");
        try {
            notificationService.sendScheduledReminders();
            log.info("Scheduled reminder check completed successfully");
        } catch (Exception e) {
            log.error("Error in scheduled reminder check", e);
        }
    }
}
