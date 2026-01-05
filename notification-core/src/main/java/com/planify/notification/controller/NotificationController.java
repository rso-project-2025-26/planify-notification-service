package com.planify.notification.controller;

import com.planify.notification.dto.NotificationTemplateRequest;
import com.planify.notification.dto.SendNotificationRequest;
import com.planify.notification.model.NotificationLog;
import com.planify.notification.model.NotificationTemplate;
import com.planify.notification.repository.NotificationLogRepository;
import com.planify.notification.repository.NotificationTemplateRepository;
import com.planify.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Administration", description = "Admin endpoints for managing notification templates and delivery logs")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final NotificationService notificationService;

    /**
     * Pridobi seznam vseh templatov za obvestila.
     */
    @GetMapping("/templates")
    @Operation(
        summary = "Get all notification templates",
        description = "Returns a list of all notification templates available for email and SMS notifications. Templates include placeholders for dynamic content."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of templates",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationTemplate.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    /**
     * Pridobimo določen template na podlagi ID-ja
     */
    @GetMapping("/templates/{id}")
    @Operation(
        summary = "Get notification template by ID",
        description = "Returns detailed information about a specific notification template identified by its UUID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved template",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationTemplate.class))),
        @ApiResponse(responseCode = "404", description = "Template not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationTemplate> getTemplate(
            @Parameter(required = true)
            @PathVariable UUID id) {
        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/templates")
    @Operation(
        summary = "Create new notification template",
        description = "Creates a new notification template with email and SMS variants. Templates support placeholder variables for dynamic content."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Template successfully created",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationTemplate.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationTemplate> createTemplate(
            @Parameter(required = true)
            @RequestBody NotificationTemplateRequest request) {

        NotificationTemplate template = new NotificationTemplate();
        template.setTemplateKey(request.getTemplateKey());
        template.setType(request.getType());
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());
        template.setSmsTemplate(request.getSmsTemplate());
        template.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        template.setLanguage(request.getLanguage() != null ? request.getLanguage() : "sl");

        NotificationTemplate saved = templateRepository.save(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/templates/{id}")
    @Operation(
        summary = "Update notification template",
        description = "Updates an existing notification template. Only provided fields will be updated."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template successfully updated",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationTemplate.class))),
        @ApiResponse(responseCode = "404", description = "Template not found", content = @Content),
        @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationTemplate> updateTemplate(
            @Parameter(required = true)
            @PathVariable UUID id,
            @Parameter(required = true)
            @RequestBody NotificationTemplateRequest request) {

        return templateRepository.findById(id)
                .map(template -> {
                    if (request.getSubject() != null)
                        template.setSubject(request.getSubject());
                    if (request.getBodyTemplate() != null)
                        template.setBodyTemplate(request.getBodyTemplate());
                    if (request.getSmsTemplate() != null)
                        template.setSmsTemplate(request.getSmsTemplate());
                    if (request.getIsActive() != null)
                        template.setIsActive(request.getIsActive());
                    if (request.getLanguage() != null)
                        template.setLanguage(request.getLanguage());

                    NotificationTemplate updated = templateRepository.save(template);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/templates/{id}")
    @Operation(
        summary = "Delete notification template",
        description = "Permanently deletes a notification template. This action cannot be undone."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Template successfully deleted", content = @Content),
        @ApiResponse(responseCode = "404", description = "Template not found", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(required = true)
            @PathVariable UUID id) {
        if (!templateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Notification Logs

    @GetMapping("/logs")
    @Operation(
        summary = "Get notification delivery logs",
        description = "Returns paginated notification delivery logs including status, timestamps, and delivery details for email and SMS notifications."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved logs",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationLog.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<Page<NotificationLog>> getAllLogs(
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable) {
        return ResponseEntity.ok(logRepository.findAll(pageable));
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(
        summary = "Get notification logs by user",
        description = "Returns all notification delivery logs for a specific user, ordered by creation date."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user logs",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationLog.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token", content = @Content),
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<List<NotificationLog>> getLogsByUser(
            @Parameter(required = true)
            @PathVariable UUID userId) {
        return ResponseEntity.ok(logRepository.findByUserId(userId));
    }
}
