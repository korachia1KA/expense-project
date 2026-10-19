package com.portfolio.expensetracker.budget.dto;

import java.math.BigDecimal;

/**
 * A budget together with how much has been spent against it:
 * <pre>{@code
 * { "id": 3, "categoryId": 1, "categoryName": "Food", "color": "#f97316", "month": "2026-10",
 *   "limit": 400.00, "spent": 352.10, "remaining": 47.90, "percentUsed": 88.0 }
 * }</pre>
 *
 * @param remaining   limit minus spent; NEGATIVE when over budget
 * @param percentUsed spent / limit × 100 (can exceed 100). The UI turns amber above 80, red above 100.
 */
public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String color,
        String month,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining,
        double percentUsed) {
}
