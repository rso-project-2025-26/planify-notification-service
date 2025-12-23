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
@Tag(name = "Notification Administration", description = "Manage templates, logs and send test notifications")
public class NotificationController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;

    /**
     * Pridobi seznam vseh templatov za obvestila.
     */
    @GetMapping("/templates")
    @Operation(summary = "List notification templates", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Templates fetched",
            content = @Content(schema = @Schema(implementation = NotificationTemplate.class))))
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<List<NotificationTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateRepository.findAll());
    }

    /**
     * Pridobimo določen template na podlagi ID-ja
     */
    @GetMapping("/templates/{id}")
    @Operation(summary = "Get a template by id", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationTemplate> getTemplate(@PathVariable UUID id) {
        return templateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/templates")
    @Operation(summary = "Create a new template", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Created"))
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationTemplate> createTemplate(
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
    @Operation(summary = "Update a template", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<NotificationTemplate> updateTemplate(
            @PathVariable UUID id,
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
    @Operation(summary = "Delete a template", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        if (!templateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Notification Logs

    @GetMapping("/logs")
    @Operation(summary = "Paginated notification logs", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<Page<NotificationLog>> getAllLogs(Pageable pageable) {
        return ResponseEntity.ok(logRepository.findAll(pageable));
    }

    @GetMapping("/logs/user/{userId}")
    @Operation(summary = "Logs by user id", security = {@SecurityRequirement(name = "roleHeaderAuth")})
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    public ResponseEntity<List<NotificationLog>> getLogsByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(logRepository.findByUserId(userId));
    }

    // TODO implementiraj opomnike
//    @PostMapping("/reminders/check")
//    public ResponseEntity<String> checkReminders() {
//        try {
//            notificationService.sendScheduledReminders();
//            return ResponseEntity.ok("Reminder check completed");
//        } catch (Exception e) {
//            log.error("Error checking reminders", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body("Error: " + e.getMessage());
//        }
//    }
}
