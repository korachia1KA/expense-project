package com.portfolio.expensetracker.summary.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Total expenses on one calendar day (0 when nothing was spent). */
public record DailySpending(LocalDate date, BigDecimal amount) {
}
