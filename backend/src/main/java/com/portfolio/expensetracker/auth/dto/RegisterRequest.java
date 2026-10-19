package com.portfolio.expensetracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * JSON body of {@code POST /api/auth/register}.
 *
 * <p>DTO = Data Transfer Object: a class whose only job is to carry data across the HTTP boundary.
 * Using DTOs instead of entities means clients can only send the fields we allow (they cannot, say,
 * set their own {@code id}), and the API shape can change independently of the database.
 *
 * <p>The annotations are Bean Validation constraints. They are checked when the controller parameter
 * is marked {@code @Valid}; failures become a 400 with {@code fieldErrors} (see GlobalExceptionHandler).
 */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password) {
}
