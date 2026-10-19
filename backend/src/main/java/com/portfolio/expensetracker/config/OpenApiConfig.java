package com.portfolio.expensetracker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata for the auto-generated API documentation (springdoc-openapi).
 *
 * <p>Open http://localhost:8082/swagger-ui.html, call POST /api/auth/login, copy the token, click
 * "Authorize" and paste it: every "Try it out" request then sends {@code Authorization: Bearer ...}.
 * The {@code @SecurityScheme} below is what makes that "Authorize" button appear.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Expense Tracker API", version = "1.0",
                description = "Track income and expenses, organise them in categories and set monthly budgets."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
