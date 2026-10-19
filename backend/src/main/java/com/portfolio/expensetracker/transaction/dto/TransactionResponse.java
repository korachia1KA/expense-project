package com.portfolio.expensetracker.transaction.dto;

import com.portfolio.expensetracker.category.Category;
import com.portfolio.expensetracker.transaction.Transaction;
import com.portfolio.expensetracker.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A transaction as returned by the API. The category is embedded as a small object so the table
 * can show a colored label without a second request:
 * <pre>{@code
 * { "id": 7, "type": "EXPENSE", "amount": 54.20, "description": "Groceries", "date": "2026-10-03",
 *   "notes": null, "category": { "id": 1, "name": "Food", "color": "#f97316" }, ... }
 * }</pre>
 */
public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate date,
        String notes,
        CategoryRef category,
        Instant createdAt,
        Instant updatedAt) {

    /** Minimal category info nested inside a transaction. */
    public record CategoryRef(Long id, String name, String color) {
        static CategoryRef from(Category c) {
            return new CategoryRef(c.getId(), c.getName(), c.getColor());
        }
    }

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getDescription(), t.getDate(),
                t.getNotes(), CategoryRef.from(t.getCategory()), t.getCreatedAt(), t.getUpdatedAt());
    }
}
