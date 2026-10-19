package com.portfolio.expensetracker.summary.dto;

import com.portfolio.expensetracker.transaction.TransactionType;

import java.math.BigDecimal;

/**
 * Row of the aggregate query "total per (year, month, type)" used for the 6-month trend chart
 * ({@code TransactionRepository.monthlyTotals}). JPQL's {@code year()} / {@code month()} functions
 * return Integers, hence the boxed types.
 */
public record MonthTypeTotal(Integer year, Integer month, TransactionType type, BigDecimal total) {
}
