package com.portfolio.expensetracker.summary;

import com.portfolio.expensetracker.budget.Budget;
import com.portfolio.expensetracker.budget.BudgetRepository;
import com.portfolio.expensetracker.common.Money;
import com.portfolio.expensetracker.common.Months;
import com.portfolio.expensetracker.summary.dto.*;
import com.portfolio.expensetracker.transaction.TransactionRepository;
import com.portfolio.expensetracker.transaction.TransactionType;
import com.portfolio.expensetracker.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the dashboard summary for one month.
 *
 * <p><b>Design:</b> the heavy lifting (adding up amounts) is done by four GROUP BY queries in
 * {@link TransactionRepository}. Each returns only a few rows (≤ 2 types, ≤ ~10 categories, ≤ 31 days,
 * ≤ 12 month×type pairs) no matter how many transactions exist. This service then does the cheap part
 * in Java: merging budgets in, computing percentages, and FILLING GAPS.
 *
 * <p><b>Why fill gaps?</b> SQL GROUP BY only returns groups that have rows. A day with no spending
 * simply doesn't appear. Charts, however, need one point per day/month, otherwise the x-axis skips
 * days and bars line up with the wrong labels. So we create every day/month with 0 and overwrite the
 * ones the database returned.
 */
@Service
@Transactional(readOnly = true)
public class SummaryService {

    /** Number of months in the income-vs-expense trend chart (including the selected month). */
    static final int TREND_MONTHS = 6;

    private final TransactionRepository transactions;
    private final BudgetRepository budgets;
    private final Clock clock;

    public SummaryService(TransactionRepository transactions, BudgetRepository budgets, Clock clock) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.clock = clock;
    }

    /**
     * @param month "YYYY-MM", or null/blank for the current month (according to the injected Clock)
     */
    public SummaryResponse summary(User user, String month) {
        YearMonth ym = Months.parseOrCurrent(month, clock);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();   // handles 28/29/30/31-day months and leap years

        // ---- 1. Totals: one query returns (INCOME, x) and/or (EXPENSE, y). ----
        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        transactions.sumByType(user.getId(), from, to).forEach(t -> totals.put(t.type(), t.total()));
        BigDecimal income = Money.orZero(totals.get(TransactionType.INCOME));
        BigDecimal expense = Money.orZero(totals.get(TransactionType.EXPENSE));
        BigDecimal balance = income.subtract(expense);
        // Savings rate = share of income that was NOT spent. Negative if you overspent.
        double savingsRate = Money.percent(balance, income);

        return new SummaryResponse(ym.toString(), income, expense, balance, savingsRate,
                spendingByCategory(user, ym, from, to),
                dailySpending(user, from, to),
                trend(user, ym));
    }

    /**
     * Expense per category, merged with the month's budgets. Categories with a budget but no spending
     * yet are included with spent = 0, so the dashboard can show "0 of 400 used".
     */
    private List<CategorySpending> spendingByCategory(User user, YearMonth ym, LocalDate from, LocalDate to) {
        Map<Long, Budget> budgetByCategory = budgets.findByUserIdAndMonth(user.getId(), ym.toString()).stream()
                .collect(Collectors.toMap(b -> b.getCategory().getId(), b -> b));

        List<CategorySpending> result = new ArrayList<>();
        // Query results are already ordered by total desc.
        for (CategoryTotal row : transactions.totalsByCategory(user.getId(), TransactionType.EXPENSE, from, to)) {
            Budget budget = budgetByCategory.remove(row.categoryId());  // remove = "handled"
            result.add(toSpending(row.categoryId(), row.name(), row.color(), row.total(), budget));
        }
        // Whatever is left in the map are budgets without spending this month.
        budgetByCategory.values().stream()
                .sorted(Comparator.comparing(b -> b.getCategory().getName()))
                .forEach(b -> result.add(toSpending(b.getCategory().getId(), b.getCategory().getName(),
                        b.getCategory().getColor(), BigDecimal.ZERO, b)));
        return result;
    }

    private static CategorySpending toSpending(Long id, String name, String color, BigDecimal spent, Budget budget) {
        BigDecimal limit = budget == null ? null : budget.getLimitAmount();
        Double percentUsed = limit == null ? null : Money.percent(spent, limit);
        return new CategorySpending(id, name, color, spent, limit, percentUsed);
    }

    /** One entry per day of the month; days without spending are 0. */
    private List<DailySpending> dailySpending(User user, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> byDay = transactions
                .dailyTotals(user.getId(), TransactionType.EXPENSE, from, to).stream()
                .collect(Collectors.toMap(DailyTotal::date, DailyTotal::total));
        // datesUntil is exclusive at the end, so add one day to include the last day of the month.
        return from.datesUntil(to.plusDays(1))
                .map(day -> new DailySpending(day, byDay.getOrDefault(day, BigDecimal.ZERO)))
                .toList();
    }

    /** Income and expense for the {@value #TREND_MONTHS} months ending with {@code last}, oldest first. */
    private List<MonthlyTrend> trend(User user, YearMonth last) {
        YearMonth first = last.minusMonths(TREND_MONTHS - 1L);

        // TreeMap keeps months sorted. Pre-fill every month with zeros: [income, expense].
        Map<YearMonth, BigDecimal[]> byMonth = new TreeMap<>();
        for (int i = 0; i < TREND_MONTHS; i++) {
            byMonth.put(first.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (MonthTypeTotal row : transactions.monthlyTotals(user.getId(), first.atDay(1), last.atEndOfMonth())) {
            BigDecimal[] cell = byMonth.get(YearMonth.of(row.year(), row.month()));
            if (cell != null) {
                cell[row.type() == TransactionType.INCOME ? 0 : 1] = Money.orZero(row.total());
            }
        }
        return byMonth.entrySet().stream()
                .map(e -> new MonthlyTrend(e.getKey().toString(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }
}
