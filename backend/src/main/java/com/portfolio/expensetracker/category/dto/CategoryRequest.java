package com.portfolio.expensetracker.category.dto;

import com.portfolio.expensetracker.transaction.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/categories} and {@code PUT /api/categories/{id}}.
 *
 * @param color must be a 6-digit hex color such as "#22c55e" (what an HTML color picker produces)
 * @param type  EXPENSE or INCOME; an unknown value fails JSON parsing → 400 "Malformed request"
 */
public record CategoryRequest(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex color like #22c55e") String color,
        @NotNull TransactionType type) {
}
