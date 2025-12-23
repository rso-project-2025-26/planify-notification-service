package com.planify.notification.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.planify.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda Handler for periodic reminder checks
 * This function is triggered by CloudWatch Events (EventBridge) on a schedule
 * Note: Not a @Component - only instantiated when deployed as Lambda
 */
@Slf4j
public class ReminderSchedulerHandler implements RequestHandler<ScheduledEvent, Map<String, Object>> {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationContext applicationContext;

    public ReminderSchedulerHandler() {
        // Initialize Spring context
        if (applicationContext == null) {
            String[] args = {};
            applicationContext = SpringApplication.run(com.planify.notification.NotificationServiceApplication.class,
                    args);
            this.notificationService = applicationContext.getBean(NotificationService.class);
        }
    }

    @Override
    public Map<String, Object> handleRequest(ScheduledEvent event, Context context) {
        log.info("Lambda function invoked at: {}", event.getTime());
        log.info("Request ID: {}", context != null ? context.getAwsRequestId() : "N/A");

        Map<String, Object> response = new HashMap<>();

        try {
            // Process scheduled reminders
            notificationService.sendScheduledReminders();

            response.put("statusCode", 200);
            response.put("message", "Scheduled reminders processed successfully");
            response.put("timestamp", event.getTime());

            log.info("Lambda function completed successfully");

        } catch (Exception e) {
            log.error("Error processing scheduled reminders", e);
            response.put("statusCode", 500);
            response.put("message", "Error: " + e.getMessage());
        }

        return response;
    }
}
