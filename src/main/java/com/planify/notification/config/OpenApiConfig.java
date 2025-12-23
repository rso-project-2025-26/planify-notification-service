package com.planify.notification.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Planify Notification Service API",
                version = "1.0",
                description = "API for managing notification templates, logs, and in-app notifications.",
                contact = @Contact(name = "Planify Team", email = "support@planify.com")
        ),
        security = {@SecurityRequirement(name = "roleHeaderAuth")}
)
@SecurityScheme(
        name = "roleHeaderAuth",
        type = SecuritySchemeType.APIKEY,
        paramName = "X-Roles",
        description = "Provide roles as comma-separated values (e.g., USER,ADMIN). Optionally add X-User-Id.",
        in = io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
