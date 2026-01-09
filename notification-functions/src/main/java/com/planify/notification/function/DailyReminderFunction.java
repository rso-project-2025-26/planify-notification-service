package com.planify.notification.function;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Azure Function za dnevno pošiljanje SMS opomnikov
 * Sproži se vsak dan ob 11:00 AM UTC z uporabo Timer Trigger
 */
public class DailyReminderFunction {
    private static final Logger logger = Logger.getLogger(DailyReminderFunction.class.getName());

    private static final String BACKEND_URL = System.getenv()
            .getOrDefault("USER_SERVICE_BASE_URL", "http://localhost:8082");

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Metrics tracking
    private static long totalInvocations = 0;
    private static long successfulInvocations = 0;
    private static long failedInvocations = 0;

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
        logger.info("Azure Function triggered at: " + LocalDateTime.now());
        logger.info("Invocation ID: " + context.getInvocationId());
        logger.info("Total invocations: " + totalInvocations);

        long executionStart = System.currentTimeMillis();

        try {
            // Call Notification Service REST API
            String endpoint = BACKEND_URL + "/api/reminders/send";
            logger.info("Calling notification service: " + endpoint);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Azure-Function-Reminder-Trigger/1.0")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            long executionTime = System.currentTimeMillis() - executionStart;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                successfulInvocations++;

                // Parse response (should be integer - number of reminders sent)
                String remindersSent = response.body();

                successfulInvocations++;
                logger.info("METRIC: execution_duration_ms=" + executionTime);
                logger.info("METRIC: reminders_sent=" + remindersSent);
                logger.info("METRIC: success_rate=" + (double)successfulInvocations / totalInvocations * 100);
                logger.info("Reminders processed successfully in " + executionTime + "ms");

            } else {
                failedInvocations++;
                logger.severe("Notification service returned error status: " + response.statusCode());
                logger.severe("Response body: " + response.body());
                logger.severe("METRIC: execution_duration_ms=" + executionTime);
                logger.severe("METRIC: failure_rate=" +
                        String.format("%.2f", (double)failedInvocations / totalInvocations * 100));
                throw new RuntimeException("Notification service returned status " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            failedInvocations++;
            long executionTime = System.currentTimeMillis() - executionStart;

            // DODAJ: Log failure metrics
            logger.severe("METRIC: execution_failed=true");
            logger.severe("METRIC: failure_rate=" + (double)failedInvocations / totalInvocations * 100);
            logger.severe("✗ Error processing scheduled reminders after " + executionTime + "ms");
            logger.severe("ERROR: " + e.getMessage());
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

        logger.info("Manual reminder trigger invoked (Invocation:  " + totalInvocations + ")");

        try {
            String endpoint = BACKEND_URL + "/api/reminders/send";
            logger.info("Calling notification service: " + endpoint);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Azure-Function-Manual-Trigger/1.0")
                    .GET()
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            long duration = System.currentTimeMillis() - startTime;

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                successfulInvocations++;

                Map<String, Object> responseBody = new HashMap<>();
                responseBody.put("status", "success");
                responseBody.put("message", "Reminders triggered successfully");
                responseBody.put("remindersSent", response.body());
                responseBody.put("timestamp", LocalDateTime.now().toString());
                responseBody.put("durationMs", duration);
                responseBody.put("invocationCount", totalInvocations);
                responseBody.put("successRate", String.format("%.2f%%",
                        (double)successfulInvocations / totalInvocations * 100));

                logger.info("Manual trigger successful in " + duration + "ms");
                logger.info("METRIC: manual_trigger_duration_ms=" + duration);
                logger.info("METRIC: manual_trigger_success=true");

                return request.createResponseBuilder(HttpStatus.OK)
                        .body(responseBody)
                        .header("Content-Type", "application/json")
                        .build();
            } else {
                failedInvocations++;

                Map<String, Object> errorBody = new HashMap<>();
                errorBody.put("status", "error");
                errorBody.put("message", "Notification service returned error");
                errorBody.put("statusCode", response.statusCode());
                errorBody.put("serviceResponse", response.body());
                errorBody.put("timestamp", LocalDateTime.now().toString());
                errorBody.put("durationMs", duration);

                logger.severe("Manual trigger failed: status " + response.statusCode());
                logger.severe("METRIC: manual_trigger_duration_ms=" + duration);
                logger.severe("METRIC: manual_trigger_failed=true");

                return request.createResponseBuilder(HttpStatus.valueOf(response.statusCode()))
                        .body(errorBody)
                        .header("Content-Type", "application/json")
                        .build();
            }

        } catch (IOException | InterruptedException e) {
            failedInvocations++;
            long duration = System.currentTimeMillis() - startTime;

            logger.severe("Manual trigger failed after " + duration);
            logger.severe("ERROR: " + e.getMessage());
            logger.severe("METRIC: manual_trigger_duration_ms=" + duration);
            logger.severe("METRIC: manual_trigger_failed=true");

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", "error");
            errorBody.put("message", e.getMessage());
            errorBody.put("errorType", e.getClass().getSimpleName());
            errorBody.put("timestamp", LocalDateTime.now().toString());
            errorBody.put("durationMs", duration);

            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody)
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
            ExecutionContext context) {

        Map<String, Object> health = new HashMap<>();
        health.put("status", "healthy");
        health.put("service", "notification-reminder-function");
        health.put("version", "2.0-lightweight");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("backendUrl", BACKEND_URL);
        health.put("totalInvocations", totalInvocations);
        health.put("successfulInvocations", successfulInvocations);
        health.put("failedInvocations", failedInvocations);

        if (totalInvocations > 0) {
            health.put("successRate", String.format("%.2f%%",
                    (double)successfulInvocations / totalInvocations * 100));
        }

        // Test connectivity to backend
        try {
            String endpoint = BACKEND_URL + "/actuator/health";
            HttpRequest testRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> testResponse = httpClient.send(testRequest,
                    HttpResponse.BodyHandlers.ofString());

            health.put("backendConnectivity", testResponse.statusCode() == 200 ? "OK" : "ERROR");
            health.put("backendStatus", testResponse.statusCode());
        } catch (Exception e) {
            health.put("backendConnectivity", "ERROR");
            health.put("backendError", e.getMessage());
        }

        return request.createResponseBuilder(HttpStatus.OK)
                .body(health)
                .header("Content-Type", "application/json")
                .build();
    }
}