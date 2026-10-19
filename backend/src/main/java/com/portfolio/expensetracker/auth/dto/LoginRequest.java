package com.portfolio.expensetracker.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body of {@code POST /api/auth/login}. Only presence is validated here; whether the
 * credentials are correct is decided by {@code AuthService}.
 */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
