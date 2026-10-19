package com.portfolio.expensetracker.summary.dto;

import com.portfolio.expensetracker.transaction.TransactionType;

import java.math.BigDecimal;

/**
 * Row of the aggregate query "total amount per transaction type in a date range"
 * ({@code TransactionRepository.sumByType}). At most two rows come back: INCOME and EXPENSE.
 */
public record TypeTotal(TransactionType type, BigDecimal total) {
}
