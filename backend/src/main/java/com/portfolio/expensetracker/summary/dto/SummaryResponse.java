package com.portfolio.expensetracker.summary.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Everything the dashboard needs for one month, in a single response (one HTTP round trip):
 *
 * @param month              the month summarised, "YYYY-MM"
 * @param totalIncome        sum of INCOME transactions in the month
 * @param totalExpense       sum of EXPENSE transactions in the month
 * @param balance            income − expense (negative = spent more than earned)
 * @param savingsRate        balance / income × 100, one decimal; 0 when there is no income
 * @param spendingByCategory expense per category (biggest first) with budget info when one exists
 * @param dailySpending      one entry for EVERY day of the month (zero days included) for the chart
 * @param trend              the 6 months ending with {@code month}, oldest first (zero months included)
 */
public record SummaryResponse(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        double savingsRate,
        List<CategorySpending> spendingByCategory,
        List<DailySpending> dailySpending,
        List<MonthlyTrend> trend) {
}
