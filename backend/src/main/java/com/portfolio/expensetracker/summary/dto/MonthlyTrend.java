package com.portfolio.expensetracker.summary.dto;

import java.math.BigDecimal;

/** Income and expense totals for one month ("YYYY-MM") of the trend chart. */
public record MonthlyTrend(String month, BigDecimal income, BigDecimal expense) {
}
