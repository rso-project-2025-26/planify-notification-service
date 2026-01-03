package com.planify.notification.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import com.planify.notification.NotificationServiceApplication;
import com.planify.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework. boot.SpringApplication;
import org.springframework. context.ConfigurableApplicationContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Azure Function za dnevno pošiljanje SMS opomnikov
 * Sproži se vsak dan ob 11:00 AM UTC z uporabo Timer Trigger
 */
@Slf4j
public class DailyReminderFunction {

    private static NotificationService notificationService;
    private static boolean initialized = false;

    // Metrics tracking
    private static long totalInvocations = 0;
    private static long successfulInvocations = 0;
    private static long failedInvocations = 0;

    // Cold start initialization
    static {
        initializeSpringContext();
    }

    private static synchronized void initializeSpringContext() {
        if (!initialized) {
            try {
                log.info("Azure Function cold start - initializing Spring Boot context");
                long startTime = System.currentTimeMillis();

                ConfigurableApplicationContext applicationContext = SpringApplication.run(NotificationServiceApplication.class);
                notificationService = applicationContext.getBean(NotificationService.class);

                long duration = System.currentTimeMillis() - startTime;
                log. info("Spring context initialized successfully in {}ms", duration);
                initialized = true;
            } catch (Exception e) {
                log.error("FATAL: Failed to initialize Spring context", e);
                throw new RuntimeException("Azure Function initialization failed", e);
            }
        }
    }

    /**
     * Timer Trigger: Vsak dan ob 11:00 AM UTC
     * Cron format: {second} {minute} {hour} {day} {month} {day-of-week}
     * "0 0 11 * * *" = Vsak dan ob 11:00:00 AM UTC
     */
    @FunctionName("DailyReminderSender")
    public void run(
            @TimerTrigger(
                    name = "timerInfo",
                    schedule = "0 0 11 * * *"  // Vsak dan ob 11:00 AM UTC
            ) String timerInfo,
            ExecutionContext context) {
        totalInvocations++;
        log.info("Azure Function triggered at:  {}", LocalDateTime.now());
        log.info("Invocation ID: {}", context.getInvocationId());
        log.info("Total invocations: {}", totalInvocations);

        long executionStart = System.currentTimeMillis();
        int remindersSent = 0;

        try {
            if (notificationService == null) {
                throw new IllegalStateException("NotificationService not initialized");
            }

            log.info("Starting reminder processing.. .");
            remindersSent = notificationService.sendScheduledReminders();

            long executionTime = System.currentTimeMillis() - executionStart;

            successfulInvocations++;
            log.info("METRIC: execution_duration_ms={}", executionTime);
            log.info("METRIC: reminders_sent={}", remindersSent);
            log.info("METRIC: success_rate={}", (double)successfulInvocations / totalInvocations * 100);
            log.info("Reminders processed successfully in {}ms", executionTime);

        } catch (Exception e) {
            failedInvocations++;
            long executionTime = System.currentTimeMillis() - executionStart;

            // DODAJ: Log failure metrics
            log.error("METRIC: execution_failed=true");
            log.error("METRIC: failure_rate={}", (double)failedInvocations / totalInvocations * 100);
            log.error("✗ Error processing scheduled reminders after {}ms", executionTime, e);
            throw new RuntimeException("Failed to process reminders", e);
        }
    }

    /**
     * HTTP-triggered za ročno testiranje
     */
    @FunctionName("ManualReminderTrigger")
    public HttpResponseMessage runManual(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION
            ) HttpRequestMessage<String> request,
            ExecutionContext context) {
        totalInvocations++;
        long startTime = System.currentTimeMillis();

        log.info("Manual reminder trigger invoked (Invocation:  {})", totalInvocations);

        try {
            if (notificationService == null) {
                throw new IllegalStateException("NotificationService not initialized");
            }

            notificationService.sendScheduledReminders();

            long duration = System.currentTimeMillis() - startTime;
            successfulInvocations++;

            // DODAJ: Log metrics
            log.info("METRIC: manual_trigger_duration_ms={}", duration);
            log.info("METRIC: manual_trigger_success=true");

            Map<String, Object> response = new HashMap<>();
            response. put("status", "success");
            response. put("message", "Reminders processed successfully");
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("duration_ms", duration);
            response.put("invocation_count", totalInvocations);
            response.put("success_rate", String.format("%.2f%%", (double)successfulInvocations / totalInvocations * 100));

            return request.createResponseBuilder(HttpStatus.OK)
                    .body(response)
                    .header("Content-Type", "application/json")
                    .build();

        } catch (Exception e) {
            failedInvocations++;
            long duration = System.currentTimeMillis() - startTime;

            // DODAJ: Log failure
            log.error("METRIC:  manual_trigger_duration_ms={}", duration);
            log.error("METRIC: manual_trigger_failed=true");
            log.error("Error in manual reminder trigger", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            errorResponse. put("timestamp", LocalDateTime.now().toString());

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse)
                    .header("Content-Type", "application/json")
                    .build();
        }
    }

    /**
     * Health check endpoint za monitoring
     */
    @FunctionName("HealthCheck")
    public HttpResponseMessage healthCheck(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "health"
            ) HttpRequestMessage<String> request,
            final ExecutionContext context) {

        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "notification-reminder");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("spring_initialized", initialized);
        health.put("total_invocations", totalInvocations);
        health.put("successful_invocations", successfulInvocations);
        health.put("failed_invocations", failedInvocations);

        if (totalInvocations > 0) {
            health. put("success_rate", String.format("%. 2f%%", (double)successfulInvocations / totalInvocations * 100));
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(health)
                .header("Content-Type", "application/json")
                .build();
    }
}