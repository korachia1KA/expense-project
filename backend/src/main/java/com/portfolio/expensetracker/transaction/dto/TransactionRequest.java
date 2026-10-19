package com.portfolio.expensetracker.transaction.dto;

import com.portfolio.expensetracker.transaction.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body of {@code POST /api/transactions} and {@code PUT /api/transactions/{id}}.
 *
 * <p>Single-field rules live here as annotations (checked by {@code @Valid} before the service runs):
 * <ul>
 *   <li>{@code @DecimalMin("0.01")} - amount must be greater than zero;</li>
 *   <li>{@code @Digits(integer = 10, fraction = 2)} - fits NUMERIC(12,2): no fractions of a cent;</li>
 *   <li>{@code @PastOrPresent} - no transactions dated in the future.</li>
 * </ul>
 * The cross-field rule "category type must equal transaction type" needs the database, so it is
 * checked in {@code TransactionService} instead.
 *
 * <p>JSON numbers such as {@code 12.5} are parsed by Jackson straight into BigDecimal without passing
 * through double, so no precision is lost on the way in.
 */
public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull
        @DecimalMin(value = "0.01", message = "must be greater than 0")
        @Digits(integer = 10, fraction = 2, message = "must have at most 10 digits and 2 decimals")
        BigDecimal amount,
        @NotBlank @Size(max = 255) String description,
        @NotNull @PastOrPresent(message = "cannot be in the future") LocalDate date,
        @NotNull Long categoryId,
        @Size(max = 1000) String notes) {
}
