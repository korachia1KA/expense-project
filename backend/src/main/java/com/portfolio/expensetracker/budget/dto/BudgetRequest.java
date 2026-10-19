package com.portfolio.expensetracker.budget.dto;

import com.portfolio.expensetracker.common.Months;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Body of {@code PUT /api/budgets}: "set the limit for this category in this month".
 *
 * @param month "YYYY-MM"; the regex comes from {@link Months#PATTERN} so it's defined once
 * @param limit the spending limit, greater than zero, at most 2 decimals
 */
public record BudgetRequest(
        @NotNull Long categoryId,
        @NotBlank @Pattern(regexp = Months.PATTERN, message = "must be in YYYY-MM format") String month,
        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 digits and 2 decimals")
        BigDecimal limit) {
}
