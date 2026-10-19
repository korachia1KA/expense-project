package com.portfolio.expensetracker.category;

import com.portfolio.expensetracker.transaction.TransactionType;

import java.util.List;

/**
 * The starter set of categories every new account receives, so a fresh user can record a transaction
 * immediately instead of facing an empty dropdown.
 *
 * <p>Kept as data (a list of records) rather than code so it is easy to read and change.
 * The colors come from the Tailwind CSS palette and were picked to be distinguishable in charts.
 */
public final class DefaultCategories {

    /** One default category definition. A nested record keeps the definition next to its only user. */
    public record Definition(String name, String color, TransactionType type) {
    }

    public static final List<Definition> ALL = List.of(
            new Definition("Food", "#f97316", TransactionType.EXPENSE),
            new Definition("Transport", "#0ea5e9", TransactionType.EXPENSE),
            new Definition("Housing", "#8b5cf6", TransactionType.EXPENSE),
            new Definition("Utilities", "#eab308", TransactionType.EXPENSE),
            new Definition("Entertainment", "#ec4899", TransactionType.EXPENSE),
            new Definition("Health", "#14b8a6", TransactionType.EXPENSE),
            new Definition("Shopping", "#6366f1", TransactionType.EXPENSE),
            new Definition("Salary", "#22c55e", TransactionType.INCOME),
            new Definition("Other", "#64748b", TransactionType.EXPENSE));

    private DefaultCategories() {
    }
}
