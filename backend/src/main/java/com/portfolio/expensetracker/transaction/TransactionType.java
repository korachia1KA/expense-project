package com.portfolio.expensetracker.transaction;

/**
 * Whether money came IN or went OUT.
 *
 * <p>Used by both {@link Transaction} and {@code Category}: every category is either an income
 * category (Salary) or an expense category (Food), and a transaction's type must match its
 * category's type (enforced in {@code TransactionService}).
 *
 * <p>Stored in the database as text ("INCOME"/"EXPENSE") thanks to {@code @Enumerated(EnumType.STRING)}.
 * Storing the name instead of the ordinal (0/1) means reordering the constants can never corrupt data.
 */
public enum TransactionType {
    INCOME,
    EXPENSE
}
