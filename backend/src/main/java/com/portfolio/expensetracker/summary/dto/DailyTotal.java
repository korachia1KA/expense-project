package com.portfolio.expensetracker.summary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Row of the aggregate query "expense total per day" ({@code TransactionRepository.dailyExpense}).
 * Days without spending have NO row; {@code SummaryService} fills those gaps with zero.
 */
public record DailyTotal(LocalDate date, BigDecimal total) {
}
