package com.portfolio.expensetracker.summary.dto;

import java.math.BigDecimal;

/**
 * Spending in one expense category for the summarised month.
 *
 * @param budget      the month's limit for this category, or {@code null} if no budget is set
 * @param percentUsed spent / budget × 100, or {@code null} when there is no budget. A boxed
 *                    {@code Double} is used precisely so that "no budget" can be expressed as null
 *                    (JSON {@code null}) rather than a misleading 0.
 */
public record CategorySpending(Long categoryId, String name, String color, BigDecimal spent,
                               BigDecimal budget, Double percentUsed) {
}
