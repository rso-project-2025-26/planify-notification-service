package com.planify.notification.controller;

import com.planify.notification.dto.NotificationTemplateRequest;
import com.planify.notification.model.NotificationTemplate;
import com.planify.notification.model.NotificationType;
import com.planify.notification.repository.NotificationLogRepository;
import com.planify.notification.repository.NotificationTemplateRepository;
import com.planify.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    private UUID templateId;
    private NotificationTemplate template;
    private NotificationTemplateRequest templateRequest;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        
        template = new NotificationTemplate();
        template.setId(templateId);
        template.setTemplateKey("TEST_TEMPLATE");
        template.setType(NotificationType.EMAIL);
        template.setSubject("Test Subject");
        template.setBodyTemplate("Test Body");
        template.setSmsTemplate("Test SMS");
        template.setIsActive(true);
        template.setLanguage("sl");

        templateRequest = new NotificationTemplateRequest();
        templateRequest.setTemplateKey("NEW_TEMPLATE");
        templateRequest.setType(NotificationType.EMAIL);
        templateRequest.setSubject("New Subject");
        templateRequest.setBodyTemplate("New Body");
        templateRequest.setSmsTemplate("New SMS");
        templateRequest.setIsActive(true);
        templateRequest.setLanguage("en");
    }

    @Test
    void getAllTemplates_shouldReturnAllTemplates() {
        // Given
        List<NotificationTemplate> templates = Arrays.asList(template);
        when(templateRepository.findAll()).thenReturn(templates);

        // When
        ResponseEntity<List<NotificationTemplate>> response = controller.getAllTemplates();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        verify(templateRepository).findAll();
    }

    @Test
    void getTemplate_shouldReturnTemplateWhenFound() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        ResponseEntity<NotificationTemplate> response = controller.getTemplate(templateId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(templateId, response.getBody().getId());
        verify(templateRepository).findById(templateId);
    }

    @Test
    void getTemplate_shouldReturnNotFoundWhenTemplateDoesNotExist() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<NotificationTemplate> response = controller.getTemplate(templateId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(templateRepository).findById(templateId);
    }

    @Test
    void createTemplate_shouldCreateAndReturnTemplate() {
        // Given
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        // When
        ResponseEntity<NotificationTemplate> response = controller.createTemplate(templateRequest);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(templateRepository).save(any(NotificationTemplate.class));
    }

    @Test
    void updateTemplate_shouldUpdateAndReturnTemplate() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        // When
        ResponseEntity<NotificationTemplate> response = controller.updateTemplate(templateId, templateRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("New Subject", template.getSubject());
        assertEquals("New Body", template.getBodyTemplate());
        verify(templateRepository).save(template);
    }

    @Test
    void updateTemplate_shouldReturnNotFoundWhenTemplateDoesNotExist() {
        // Given
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        // When
        ResponseEntity<NotificationTemplate> response = controller.updateTemplate(templateId, templateRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_shouldUpdateOnlyProvidedFields() {
        // Given
        NotificationTemplateRequest partialRequest = new NotificationTemplateRequest();
        partialRequest.setSubject("Updated Subject Only");
        
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(template);

        // When
        ResponseEntity<NotificationTemplate> response = controller.updateTemplate(templateId, partialRequest);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Subject Only", template.getSubject());
        assertEquals("Test Body", template.getBodyTemplate()); // Unchanged
        verify(templateRepository).save(template);
    }

    @Test
    void deleteTemplate_shouldDeleteWhenTemplateExists() {
        // Given
        when(templateRepository.existsById(templateId)).thenReturn(true);

        // When
        ResponseEntity<Void> response = controller.deleteTemplate(templateId);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(templateRepository).deleteById(templateId);
    }

    @Test
    void deleteTemplate_shouldReturnNotFoundWhenTemplateDoesNotExist() {
        // Given
        when(templateRepository.existsById(templateId)).thenReturn(false);

        // When
        ResponseEntity<Void> response = controller.deleteTemplate(templateId);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(templateRepository, never()).deleteById(any());
    }
}
