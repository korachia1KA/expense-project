package com.portfolio.expensetracker.summary.dto;

import java.math.BigDecimal;

/**
 * Row of the aggregate query "expense total per category in a date range"
 * ({@code TransactionRepository.expenseByCategory}). Name and color are selected too, so the caller
 * does not need a second query to label the chart.
 */
public record CategoryTotal(Long categoryId, String name, String color, BigDecimal total) {
}
